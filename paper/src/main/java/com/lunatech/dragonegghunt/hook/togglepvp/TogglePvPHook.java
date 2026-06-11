package com.lunatech.dragonegghunt.hook.togglepvp;

import com.lunatech.dragonegghunt.AbstractPlugin;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.hook.AbstractHook;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.service.CombatSessionService;
import me.taucu.togglepvp.TogglePvpAPI;
import me.taucu.togglepvp.events.DamageHandleEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Hook to interface with TogglePvp to coordinate combat sessions and prevent spam/warnings.
 */
public final class TogglePvPHook extends AbstractHook implements Listener {

    private final EggTrackerService eggTrackerService;
    private final CombatSessionService combatSessionService;
    private boolean loaded = false;

    /**
     * Instantiates a new TogglePvp hook.
     *
     * @param plugin the plugin instance
     */
    public TogglePvPHook(DragonEggHunt plugin) {
        super(plugin);
        this.eggTrackerService = plugin.getEggTrackerService();
        this.combatSessionService = plugin.getCombatSessionService();
    }

    @Override
    public boolean isHookLoaded() {
        return loaded;
    }

    @Override
    public void onEnable(AbstractPlugin plugin) {
        if (isPluginEnabled("TogglePvp")) {
            loaded = true;
            getPlugin().getSLF4JLogger().info("TogglePvp integration hook enabled.");
        }
    }

    /**
     * Intercepts TogglePvp's internal check event to bypass PvP restrictions for egg holder and hunters.
     *
     * @param event the DamageHandleEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageHandle(DamageHandleEvent event) {
        if (!isHookLoaded()) {
            return;
        }

        if (!(event.getResolvedVictim() instanceof Player victim) 
                || !(event.getResolvedDamager() instanceof Player damager)) {
            return;
        }

        boolean isVictimHolder = eggTrackerService.isPvpForced(victim.getUniqueId());
        boolean isDamagerHolder = eggTrackerService.isPvpForced(damager.getUniqueId());

        if (!isVictimHolder && !isDamagerHolder) {
            return; // Neither player is the egg holder
        }

        // Only override togglepvp checks if overrideRegionProtection is enabled
        if (!eggTrackerService.isOverrideRegionProtection()) {
            return;
        }

        // Always cancel TogglePvP's check to give DragonEggHunt sole authority over holder PvP rules
        event.setCancelled(true);

        // Sync with togglepvp's native combat system so they are tagged for combat-logging and command blocks
        if (isVictimHolder || combatSessionService.isInCombat(victim.getUniqueId())) {
            triggerTogglePvpCooldown(damager, victim);
        }
    }

    private void triggerTogglePvpCooldown(Player a, Player b) {
        try {
            TogglePvpAPI api = TogglePvpAPI.get();
            java.time.Duration duration = api.getCombatCooldown();
            api.setCombatCooldown(a, duration);
            api.setCombatCooldown(b, duration);
        } catch (Throwable t) {
            // Fail-safe in case of API version mismatches
        }
    }
}
