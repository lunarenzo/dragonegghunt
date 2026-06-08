package com.lunatech.dragonegghunt.listener;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggStateListener;
import com.lunatech.dragonegghunt.state.EggState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Dispatches transitions of the Dragon Egg state to the EggAuditService.
 * Accesses Bukkit API on the primary thread to resolve coordinates safely.
 */
public final class AuditEventDispatcher implements EggStateListener {

    private final DragonEggHunt plugin;

    public AuditEventDispatcher(DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onStateTransition(EggState oldState, EggState newState) {
        // Enforce execution on the primary thread to safely access Bukkit entity/player API
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> handleTransition(oldState, newState));
        } else {
            handleTransition(oldState, newState);
        }
    }

    private void handleTransition(EggState oldState, EggState newState) {
        // Clear active combat sessions if the holder changes or the egg is no longer held
        if (!(newState instanceof EggState.Held newHeld) 
                || !(oldState instanceof EggState.Held oldHeld) 
                || !newHeld.holderUuid().equals(oldHeld.holderUuid())) {
            plugin.getCombatSessionService().clearSessions();
        }

        if (newState instanceof EggState.Held held) {
            Player player = Bukkit.getPlayer(held.holderUuid());
            if (player != null && player.isOnline()) {
                var loc = player.getLocation();
                plugin.getEggAuditService().logTransition(
                    "PICKUP",
                    held.holderUuid(),
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ()
                );
            } else {
                plugin.getEggAuditService().logTransition(
                    "PICKUP",
                    held.holderUuid(),
                    "unknown",
                    0.0,
                    0.0,
                    0.0
                );
            }
        } else if (newState instanceof EggState.Placed placed) {
            plugin.getEggAuditService().logTransition(
                "PLACE",
                null,
                placed.worldName(),
                placed.x(),
                placed.y(),
                placed.z()
            );
        } else if (newState instanceof EggState.Dropped dropped) {
            Entity entity = Bukkit.getEntity(dropped.entityUuid());
            if (entity != null && entity.isValid()) {
                var loc = entity.getLocation();
                plugin.getEggAuditService().logTransition(
                    "DROP",
                    null,
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ()
                );
            } else {
                plugin.getEggAuditService().logTransition(
                    "DROP",
                    null,
                    dropped.worldName(),
                    dropped.x(),
                    dropped.y(),
                    dropped.z()
                );
            }
        } else if (newState instanceof EggState.Unheld) {
            // Reconstruct location from previous state if possible to know where it was lost
            String world = "unknown";
            double x = 0, y = 0, z = 0;
            UUID playerUuid = null;

            if (oldState instanceof EggState.Held held) {
                playerUuid = held.holderUuid();
                Player player = Bukkit.getPlayer(held.holderUuid());
                if (player != null && player.isOnline()) {
                    var loc = player.getLocation();
                    world = loc.getWorld().getName();
                    x = loc.getX();
                    y = loc.getY();
                    z = loc.getZ();
                }
            } else if (oldState instanceof EggState.Placed placed) {
                world = placed.worldName();
                x = placed.x();
                y = placed.y();
                z = placed.z();
            } else if (oldState instanceof EggState.Dropped dropped) {
                world = dropped.worldName();
                x = dropped.x();
                y = dropped.y();
                z = dropped.z();
            }

            plugin.getEggAuditService().logTransition(
                "UNHELD",
                playerUuid,
                world,
                x,
                y,
                z
            );
        }
    }
}
