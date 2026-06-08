package com.lunatech.dragonegghunt.listener;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.service.CombatSessionService;
import me.taucu.togglepvp.TogglePvpAPI;
import me.taucu.togglepvp.events.DamageHandleEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener to intercept togglepvp's internal checks and prevent spam/warnings.
 * This class is only loaded if the togglepvp plugin is enabled.
 */
public final class TogglePvpListener implements Listener {

    private final EggTrackerService eggTrackerService;
    private final CombatSessionService combatSessionService;

    public TogglePvpListener(DragonEggHunt plugin) {
        this.eggTrackerService = plugin.getEggTrackerService();
        this.combatSessionService = plugin.getCombatSessionService();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageHandle(DamageHandleEvent event) {
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

        if (isVictimHolder) {
            // Case 1: Hunter attacks the Egg Holder.
            // Cancel togglepvp's check to prevent it from blocking and sending warning messages.
            event.setCancelled(true);
            
            // Sync with togglepvp's native combat system so they are tagged for combat-logging and command blocks
            triggerTogglePvpCooldown(damager, victim);
        } else {
            // Case 2: Egg Holder attacks a Hunter.
            // Only allow (cancel togglepvp's check) if the hunter is already in active combat.
            if (combatSessionService.isInCombat(victim.getUniqueId())) {
                event.setCancelled(true);
                
                // Refresh togglepvp's native combat cooldown
                triggerTogglePvpCooldown(damager, victim);
            }
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
