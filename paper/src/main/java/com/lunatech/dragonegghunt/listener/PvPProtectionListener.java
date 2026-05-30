package com.lunatech.dragonegghunt.listener;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener to enforce PvP rules for the Dragon Egg holder.
 */
public class PvPProtectionListener implements Listener {

    private final EggTrackerService eggTrackerService;

    public PvPProtectionListener(DragonEggHunt plugin) {
        this.eggTrackerService = plugin.getEggTrackerService();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player damager)) {
            return;
        }

        boolean involvesHolder = eggTrackerService.isPvpForced(victim.getUniqueId())
            || eggTrackerService.isPvpForced(damager.getUniqueId());

        if (involvesHolder) {
            // Force PvP to be enabled for the fight
            if (event.isCancelled() && eggTrackerService.isOverrideRegionProtection()) {
                event.setCancelled(false);
            }
        }
    }
}
