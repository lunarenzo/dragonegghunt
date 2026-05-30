package com.lunatech.dragonegghunt.task;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.state.EggState;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.wordweaver.Translation;
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            EggState state = eggTrackerService.getState();
            Component message = null;

            if (state instanceof EggState.Held held) {
                Player holder = Bukkit.getPlayer(held.holderUuid());
                if (holder != null && holder.isOnline()) {
                    var loc = holder.getLocation();
                    message = ColorParser.of(Translation.of("egghunt.actionbar.held-online"))
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
                    message = ColorParser.of(Translation.of("egghunt.actionbar.held-offline"))
                        .with("player", offlineName)
                        .build();
                }
            } else if (state instanceof EggState.Placed placed) {
                message = ColorParser.of(Translation.of("egghunt.actionbar.placed"))
                    .with("x", String.valueOf((int) placed.x()))
                    .with("y", String.valueOf((int) placed.y()))
                    .with("z", String.valueOf((int) placed.z()))
                    .with("world", placed.worldName())
                    .build();
            } else if (state instanceof EggState.Dropped dropped) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(dropped.entityUuid());
                if (entity != null && entity.isValid() && !entity.isDead()) {
                    var loc = entity.getLocation();
                    message = ColorParser.of(Translation.of("egghunt.actionbar.dropped"))
                        .with("x", String.valueOf(loc.getBlockX()))
                        .with("y", String.valueOf(loc.getBlockY()))
                        .with("z", String.valueOf(loc.getBlockZ()))
                        .with("world", loc.getWorld().getName())
                        .build();
                } else {
                    message = ColorParser.of(Translation.of("egghunt.actionbar.dropped"))
                        .with("x", String.valueOf((int) dropped.x()))
                        .with("y", String.valueOf((int) dropped.y()))
                        .with("z", String.valueOf((int) dropped.z()))
                        .with("world", dropped.worldName())
                        .build();
                }
            }

            if (message != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendActionBar(message);
                }
            }
        });
    }
}
