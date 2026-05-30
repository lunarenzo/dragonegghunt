package com.lunatech.dragonegghunt.task;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.state.EggState;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Task to periodically broadcast the Dragon Egg's location or holder info to all online players.
 */
public class TrackerBroadcastTask implements Runnable {

    private final DragonEggHunt plugin;
    private final EggTrackerService eggTrackerService;

    public TrackerBroadcastTask(DragonEggHunt plugin) {
        this.plugin = plugin;
        this.eggTrackerService = plugin.getEggTrackerService();
    }

    @Override
    public void run() {
        // Scan online players to keep possession state in sync (handles /give, /clear, creative actions)
        Player actualHolder = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getInventory().contains(org.bukkit.Material.DRAGON_EGG) || 
                (player.getItemOnCursor() != null && player.getItemOnCursor().getType() == org.bukkit.Material.DRAGON_EGG)) {
                actualHolder = player;
                break;
            }
        }

        EggState state = eggTrackerService.getState();

        if (actualHolder != null) {
            if (!(state instanceof EggState.Held held) || !held.holderUuid().equals(actualHolder.getUniqueId())) {
                eggTrackerService.updateState(new EggState.Held(actualHolder.getUniqueId(), System.currentTimeMillis()));
                state = eggTrackerService.getState();
            }
        } else {
            // If the state says someone is holding it, but they are online and don't have it, mark as Unheld
            if (state instanceof EggState.Held held) {
                Player onlineHolder = Bukkit.getPlayer(held.holderUuid());
                if (onlineHolder != null && onlineHolder.isOnline()) {
                    eggTrackerService.updateState(new EggState.Unheld());
                    state = eggTrackerService.getState();
                }
            }
        }

        Component message = null;

        if (state instanceof EggState.Held held) {
            Player holder = Bukkit.getPlayer(held.holderUuid());
            if (holder != null && holder.isOnline()) {
                var loc = holder.getLocation();
                message = ColorParser.of("<gold>Dragon Egg Holder: <yellow><player> <gray>(<white><x>, <y>, <z> in <world><gray>)")
                    .with("player", holder.getName())
                    .with("x", String.valueOf(loc.getBlockX()))
                    .with("y", String.valueOf(loc.getBlockY()))
                    .with("z", String.valueOf(loc.getBlockZ()))
                    .with("world", loc.getWorld().getName())
                    .build();
            } else {
                String offlineName = Bukkit.getOfflinePlayer(held.holderUuid()).getName();
                if (offlineName == null) {
                    offlineName = "Unknown";
                }
                message = ColorParser.of("<gold>Dragon Egg Holder: <yellow><player> <red>(Offline)")
                    .with("player", offlineName)
                    .build();
            }
        } else if (state instanceof EggState.Placed placed) {
            message = ColorParser.of("<gold>Dragon Egg placed at: <yellow><x>, <y>, <z> <gray>in <white><world>")
                .with("x", String.valueOf((int) placed.x()))
                .with("y", String.valueOf((int) placed.y()))
                .with("z", String.valueOf((int) placed.z()))
                .with("world", placed.worldName())
                .build();
        }

        if (message != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(message);
            }
        }
    }
}
