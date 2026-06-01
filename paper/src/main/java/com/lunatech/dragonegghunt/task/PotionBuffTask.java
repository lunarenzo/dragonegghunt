package com.lunatech.dragonegghunt.task;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.state.EggState;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Task to periodically apply configured potion buffs to the player currently holding the Alpha Dragon Egg.
 */
public final class PotionBuffTask implements Runnable {

    private final DragonEggHunt plugin;
    private final EggTrackerService eggTrackerService;
    private final List<PotionEffect> parsedEffects = new ArrayList<>();
    private UUID lastHolderUuid = null;

    public PotionBuffTask(DragonEggHunt plugin) {
        this.plugin = plugin;
        this.eggTrackerService = plugin.getEggTrackerService();
        parseConfiguredEffects();
    }

    private void parseConfiguredEffects() {
        var trackerConfig = plugin.getConfigHandler().getConfig().dragonEggTracker;
        if (trackerConfig == null) {
            return;
        }
        var rewardConfig = trackerConfig.potionBuffReward;
        if (rewardConfig == null || !rewardConfig.enabled || rewardConfig.effects == null) {
            return;
        }

        for (String entry : rewardConfig.effects) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split(":");
            String effectName = parts[0].trim().toLowerCase();
            int amplifier = 0;
            if (parts.length > 1) {
                try {
                    amplifier = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    plugin.getSLF4JLogger().warn("Invalid amplifier in potion buff config: '{}', defaulting to 0", entry);
                }
            }

            try {
                NamespacedKey key = NamespacedKey.minecraft(effectName);
                PotionEffectType type = Registry.EFFECT.get(key);
                if (type != null) {
                    // Apply for 60 ticks (3 seconds) to ensure a smooth, overlapping application without screen flickering
                    parsedEffects.add(new PotionEffect(type, 60, amplifier, true, true, true));
                } else {
                    plugin.getSLF4JLogger().warn("Unknown potion effect type config entry: '{}'", entry);
                }
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("Failed to parse potion effect: '{}'", entry, e);
            }
        }
    }

    @Override
    public void run() {
        // Run on the main thread (Bukkit/Paper API for applying/removing potion effects is not thread-safe)
        Bukkit.getScheduler().runTask(plugin, () -> {
            var trackerConfig = plugin.getConfigHandler().getConfig().dragonEggTracker;
            if (trackerConfig == null) {
                clearLastHolderEffects();
                return;
            }
            var rewardConfig = trackerConfig.potionBuffReward;
            if (rewardConfig == null || !rewardConfig.enabled || parsedEffects.isEmpty()) {
                clearLastHolderEffects();
                return;
            }

            EggState state = eggTrackerService.getState();
            if (state instanceof EggState.Held held) {
                UUID currentHolderUuid = held.holderUuid();

                // If the holder has changed, clean up effects from the previous holder
                if (lastHolderUuid != null && !lastHolderUuid.equals(currentHolderUuid)) {
                    removeEffectsFromPlayer(lastHolderUuid);
                }

                Player player = Bukkit.getPlayer(currentHolderUuid);
                if (player != null && player.isOnline()) {
                    for (PotionEffect effect : parsedEffects) {
                        player.addPotionEffect(effect);
                    }
                    lastHolderUuid = currentHolderUuid;
                } else {
                    lastHolderUuid = null;
                }
            } else {
                clearLastHolderEffects();
            }
        });
    }

    private void clearLastHolderEffects() {
        if (lastHolderUuid != null) {
            removeEffectsFromPlayer(lastHolderUuid);
            lastHolderUuid = null;
        }
    }

    private void removeEffectsFromPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            for (PotionEffect effect : parsedEffects) {
                player.removePotionEffect(effect.getType());
            }
        }
    }

    /**
     * Cleans up effects immediately (useful for shutdown/reloads).
     */
    public void cleanup() {
        clearLastHolderEffects();
    }
}
