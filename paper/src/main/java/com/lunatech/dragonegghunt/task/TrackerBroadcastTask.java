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
            org.bukkit.Location targetLoc = null;

            if (state instanceof EggState.Held held) {
                Player holder = Bukkit.getPlayer(held.holderUuid());
                if (holder != null && holder.isOnline()) {
                    var loc = holder.getLocation();
                    targetLoc = loc;
                    message = ColorParser.of(Translation.of("egghunt.actionbar.held-online"))
                        .with("player", holder.getName())
                        .with("x", String.valueOf(loc.getBlockX()))
                        .with("y", String.valueOf(loc.getBlockY()))
                        .with("z", String.valueOf(loc.getBlockZ()))
                        .with("world", com.lunatech.dragonegghunt.utility.WorldUtil.getWorldDisplayName(plugin, loc.getWorld().getName()))
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
                org.bukkit.World world = Bukkit.getWorld(placed.worldName());
                if (world != null) {
                    targetLoc = new org.bukkit.Location(world, placed.x(), placed.y(), placed.z());
                }
                message = ColorParser.of(Translation.of("egghunt.actionbar.placed"))
                    .with("x", String.valueOf((int) placed.x()))
                    .with("y", String.valueOf((int) placed.y()))
                    .with("z", String.valueOf((int) placed.z()))
                    .with("world", com.lunatech.dragonegghunt.utility.WorldUtil.getWorldDisplayName(plugin, placed.worldName()))
                    .build();
            } else if (state instanceof EggState.Dropped dropped) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(dropped.entityUuid());
                if (entity != null && entity.isValid() && !entity.isDead()) {
                    var loc = entity.getLocation();
                    targetLoc = loc;
                    message = ColorParser.of(Translation.of("egghunt.actionbar.dropped"))
                        .with("x", String.valueOf(loc.getBlockX()))
                        .with("y", String.valueOf(loc.getBlockY()))
                        .with("z", String.valueOf(loc.getBlockZ()))
                        .with("world", com.lunatech.dragonegghunt.utility.WorldUtil.getWorldDisplayName(plugin, loc.getWorld().getName()))
                        .build();
                } else {
                    org.bukkit.World world = Bukkit.getWorld(dropped.worldName());
                    if (world != null) {
                        targetLoc = new org.bukkit.Location(world, dropped.x(), dropped.y(), dropped.z());
                    }
                    message = ColorParser.of(Translation.of("egghunt.actionbar.dropped"))
                        .with("x", String.valueOf((int) dropped.x()))
                        .with("y", String.valueOf((int) dropped.y()))
                        .with("z", String.valueOf((int) dropped.z()))
                        .with("world", com.lunatech.dragonegghunt.utility.WorldUtil.getWorldDisplayName(plugin, dropped.worldName()))
                        .build();
                }
            }

            String trackingMethod = plugin.getConfigHandler().getConfig().dragonEggTracker.trackingMethod;
            boolean doCompass = "COMPASS".equalsIgnoreCase(trackingMethod) || "BOTH".equalsIgnoreCase(trackingMethod);
            boolean doActionBar = "ACTIONBAR".equalsIgnoreCase(trackingMethod) || "BOTH".equalsIgnoreCase(trackingMethod);

            if (doCompass && targetLoc != null) {
                com.lunatech.dragonegghunt.service.TrackerRecipeService recipeService = plugin.getTrackerRecipeService();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player == null) continue;
                    org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                    org.bukkit.inventory.ItemStack mainHand = inv.getItemInMainHand();
                    org.bukkit.inventory.ItemStack offHand = inv.getItemInOffHand();

                    if (recipeService.isTrackerCompass(mainHand)) {
                        updateCompassItem(mainHand, targetLoc);
                        inv.setItemInMainHand(mainHand);
                    }
                    if (recipeService.isTrackerCompass(offHand)) {
                        updateCompassItem(offHand, targetLoc);
                        inv.setItemInOffHand(offHand);
                    }
                }
            }

            if (doActionBar && message != null) {
                boolean onlyHolders = plugin.getConfigHandler().getConfig().dragonEggTracker.actionbarOnlyForHolders;
                com.lunatech.dragonegghunt.service.TrackerRecipeService recipeService = plugin.getTrackerRecipeService();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player == null) continue;

                    if (onlyHolders) {
                        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                        if (!recipeService.isTrackerCompass(inv.getItemInMainHand()) && 
                            !recipeService.isTrackerCompass(inv.getItemInOffHand())) {
                            continue;
                        }
                    }

                    player.sendActionBar(message);
                }
            }
        });
    }

    private void updateCompassItem(org.bukkit.inventory.ItemStack compass, org.bukkit.Location targetLoc) {
        org.bukkit.inventory.meta.ItemMeta meta = compass.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.CompassMeta compassMeta) {
            compassMeta.setLodestone(targetLoc);
            compassMeta.setLodestoneTracked(false);
            compass.setItemMeta(compassMeta);
        }
    }
}
