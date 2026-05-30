package io.github.exampleuser.example.task;

import io.github.exampleuser.example.Example;
import io.github.exampleuser.example.service.EggTrackerService;
import io.github.exampleuser.example.state.EggState;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Task to periodically broadcast the Dragon Egg's location or holder info to all online players.
 */
public class TrackerBroadcastTask implements Runnable {

    private final Example plugin;
    private final EggTrackerService eggTrackerService;

    public TrackerBroadcastTask(Example plugin) {
        this.plugin = plugin;
        this.eggTrackerService = plugin.getEggTrackerService();
    }

    @Override
    public void run() {
        EggState state = eggTrackerService.getState();
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
