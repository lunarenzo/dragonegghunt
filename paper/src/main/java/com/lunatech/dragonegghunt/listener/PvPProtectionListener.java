package com.lunatech.dragonegghunt.listener;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.service.CombatSessionService;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.wordweaver.Translation;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener to enforce PvP rules for the Dragon Egg holder.
 */
public class PvPProtectionListener implements Listener {

    private final DragonEggHunt plugin;
    private final EggTrackerService eggTrackerService;
    private final CombatSessionService combatSessionService;

    public PvPProtectionListener(DragonEggHunt plugin) {
        this.plugin = plugin;
        this.eggTrackerService = plugin.getEggTrackerService();
        this.combatSessionService = plugin.getCombatSessionService();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player damager = switch (event.getDamager()) {
            case Player player -> player;
            case Projectile projectile when projectile.getShooter() instanceof Player shooter -> shooter;
            default -> null;
        };

        if (damager == null || damager.equals(victim)) {
            return;
        }

        boolean isVictimHolder = eggTrackerService.isPvpForced(victim.getUniqueId());
        boolean isDamagerHolder = eggTrackerService.isPvpForced(damager.getUniqueId());

        if (!isVictimHolder && !isDamagerHolder) {
            return; // Neither player is the egg holder
        }

        // Only enforce overrides if enabled in the config
        if (!eggTrackerService.isOverrideRegionProtection()) {
            return;
        }

        if (isVictimHolder) {
            // Case 1: Hunter (damager) attacks the Egg Holder (victim)
            // This is a valid initiation. We tag the hunter and allow the damage.
            combatSessionService.registerCombat(damager.getUniqueId());
            if (event.isCancelled()) {
                event.setCancelled(false);
            }
        } else {
            // Case 2: Egg Holder (damager) attacks a Normal Player (victim)
            // We only allow this if the victim has already initiated combat.
            if (combatSessionService.isInCombat(victim.getUniqueId())) {
                // Refresh the combat tag to extend the fight
                combatSessionService.registerCombat(victim.getUniqueId());
                if (event.isCancelled()) {
                    event.setCancelled(false);
                }
            } else {
                // Holder is attacking a peaceful player who hasn't hit them.
                // Keep the event cancelled (respecting GriefPrevention / togglepvp) and notify the holder
                event.setCancelled(true);
                damager.sendMessage(ColorParser.of(Translation.of("egghunt.pvp-blocked-peaceful"))
                    .with("player", victim.getName())
                    .build());
            }
        }
    }
}
