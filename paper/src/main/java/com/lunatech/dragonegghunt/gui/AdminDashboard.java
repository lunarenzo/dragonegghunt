package com.lunatech.dragonegghunt.gui;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.persistence.TransitionLog;
import com.lunatech.dragonegghunt.state.EggState;
import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Modern inventory-based administration dashboard UI built using TriumphGUI.
 */
public final class AdminDashboard {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final java.time.format.DateTimeFormatter TIME_FORMAT = 
        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault());

    // Pre-deserialized components for zero parsing overhead on GUI open
    private static final Component TITLE_COMP = MM.deserialize("<gold><bold>Dragon Egg Hunt Admin");
    private static final Component EGG_STATUS_NAME = MM.deserialize("<light_purple><bold>Alpha Dragon Egg State");
    private static final Component EGG_STATUS_LORE_HEADER = MM.deserialize("<gray>Inspect the current physical state of the egg.");
    
    private static final Component ADMIN_ACTIONS_NAME = MM.deserialize("<red><bold>Administrative Actions");
    private static final Component ADMIN_ACTIONS_LORE_HEADER = MM.deserialize("<gray>Perform manual overrides on the egg hunt event.");
    private static final Component ADMIN_ACTIONS_LEFT_CLICK = MM.deserialize("<yellow>Left-Click: <green>Force Altar Respawn");
    private static final Component ADMIN_ACTIONS_RIGHT_CLICK = MM.deserialize("<yellow>Right-Click: <green>Locate & Get Coordinates");
    
    private static final Component AUDIT_LOGS_NAME = MM.deserialize("<aqua><bold>Recent Egg Transitions");
    private static final Component AUDIT_LOGS_LORE_HEADER = MM.deserialize("<gray>Real-time lifecycle timeline (last 10 transitions):");
    private static final Component AUDIT_LOGS_EMPTY = MM.deserialize("<red>No logs recorded yet.");

    private final DragonEggHunt plugin;

    public AdminDashboard(DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the administration dashboard GUI for the specified player.
     *
     * @param player the player to open the GUI for
     */
    public void open(Player player) {
        // Create 3-row GUI
        Gui gui = Gui.gui()
            .title(TITLE_COMP)
            .rows(3)
            .disableAllInteractions()
            .create();

        // Fill background borders
        GuiItem border = PaperItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
            .name(Component.empty())
            .asGuiItem();
        gui.getFiller().fill(border);

        // Slot 10: Egg Status & Location
        gui.setItem(10, getEggStatusItem(player, gui));

        // Slot 12: Admin Actions
        gui.setItem(12, getAdminActionsItem(player, gui));

        // Slot 14: Transition Audit Logs
        gui.setItem(14, getAuditLogsItem());

        gui.open(player);
    }

    private GuiItem getEggStatusItem(Player player, Gui gui) {
        EggState state = plugin.getEggTrackerService().getState();
        List<Component> lore = new ArrayList<>();
        org.bukkit.Location targetLoc = null;

        lore.add(EGG_STATUS_LORE_HEADER);
        lore.add(Component.empty());

        if (state instanceof EggState.Held held) {
            String name = Bukkit.getOfflinePlayer(held.holderUuid()).getName();
            lore.add(MM.deserialize("<yellow>State: <green>Held by <gold>" + (name != null ? name : "Unknown") + "</gold>"));
            Player holder = Bukkit.getPlayer(held.holderUuid());
            if (holder != null && holder.isOnline()) {
                targetLoc = holder.getLocation();
                lore.add(MM.deserialize("<yellow>Holder: <green>Online"));
                lore.add(MM.deserialize("<yellow>Location: <white>" + formatLocation(targetLoc) + "</white>"));
            } else {
                lore.add(MM.deserialize("<yellow>Holder: <red>Offline"));
            }
        } else if (state instanceof EggState.Placed placed) {
            org.bukkit.World world = Bukkit.getWorld(placed.worldName());
            if (world != null) {
                targetLoc = new org.bukkit.Location(world, placed.x(), placed.y(), placed.z());
            }
            lore.add(MM.deserialize("<yellow>State: <green>Placed (Block)"));
            lore.add(MM.deserialize("<yellow>Location: <white>" + placed.worldName() + " (" + (int)placed.x() + ", " + (int)placed.y() + ", " + (int)placed.z() + ")</white>"));
        } else if (state instanceof EggState.Dropped dropped) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(dropped.entityUuid());
            if (entity != null && entity.isValid()) {
                targetLoc = entity.getLocation();
                lore.add(MM.deserialize("<yellow>State: <green>Dropped (Entity)"));
                lore.add(MM.deserialize("<yellow>Location: <white>" + formatLocation(targetLoc) + "</white>"));
            } else {
                lore.add(MM.deserialize("<yellow>State: <green>Dropped (Unloaded / Location)"));
                lore.add(MM.deserialize("<yellow>Location: <white>" + dropped.worldName() + " (" + (int)dropped.x() + ", " + (int)dropped.y() + ", " + (int)dropped.z() + ")</white>"));
            }
        } else {
            lore.add(MM.deserialize("<yellow>State: <red>Unheld / Not Spawned"));
        }

        if (targetLoc != null) {
            lore.add(Component.empty());
            lore.add(MM.deserialize("<yellow>Left-Click to Teleport to the Egg"));
        }

        org.bukkit.Location finalLoc = targetLoc;

        return PaperItemBuilder.from(Material.DRAGON_EGG)
            .name(EGG_STATUS_NAME)
            .lore(lore)
            .asGuiItem(event -> {
                if (finalLoc != null) {
                    player.teleport(finalLoc);
                    player.sendMessage(MM.deserialize("<green>Teleported to the Alpha Dragon Egg!"));
                    gui.close(player);
                } else {
                    player.sendMessage(MM.deserialize("<red>Could not locate the egg to teleport."));
                }
            });
    }

    private GuiItem getAdminActionsItem(Player player, Gui gui) {
        List<Component> lore = new ArrayList<>();
        lore.add(ADMIN_ACTIONS_LORE_HEADER);
        lore.add(Component.empty());
        lore.add(ADMIN_ACTIONS_LEFT_CLICK);
        lore.add(ADMIN_ACTIONS_RIGHT_CLICK);

        return PaperItemBuilder.from(Material.COMMAND_BLOCK)
            .name(ADMIN_ACTIONS_NAME)
            .lore(lore)
            .asGuiItem(event -> {
                if (event.isLeftClick()) {
                    var config = plugin.getConfigHandler().getConfig().dragonEggTracker.altarLocation;
                    if (config != null) {
                        org.bukkit.World world = Bukkit.getWorld(config.world);
                        if (world != null) {
                            org.bukkit.Location altar = new org.bukkit.Location(world, config.x, config.y, config.z);
                            altar.getBlock().setType(Material.DRAGON_EGG);
                            plugin.getEggTrackerService().updateState(new EggState.Placed(config.world, config.x, config.y, config.z));

                            Bukkit.broadcast(MM.deserialize("<light_purple>The Alpha Dragon Egg has been administratively respawned at the altar!"));
                        } else {
                            player.sendMessage(MM.deserialize("<red>Altar world '" + config.world + "' is not loaded!"));
                        }
                    }
                    gui.close(player);
                } else if (event.isRightClick()) {
                    EggState state = plugin.getEggTrackerService().getState();
                    if (state instanceof EggState.Held held) {
                        String name = Bukkit.getOfflinePlayer(held.holderUuid()).getName();
                        player.sendMessage(MM.deserialize("<yellow>Egg is currently held by player: <gold>" + (name != null ? name : held.holderUuid()) + "</gold>"));
                    } else if (state instanceof EggState.Placed placed) {
                        player.sendMessage(MM.deserialize("<yellow>Egg is placed at: <gold>" + placed.worldName() + " (" + (int)placed.x() + ", " + (int)placed.y() + ", " + (int)placed.z() + ")</gold>"));
                    } else if (state instanceof EggState.Dropped dropped) {
                        player.sendMessage(MM.deserialize("<yellow>Egg is dropped at: <gold>" + dropped.worldName() + " (" + (int)dropped.x() + ", " + (int)dropped.y() + ", " + (int)dropped.z() + ")</gold>"));
                    } else {
                        player.sendMessage(MM.deserialize("<red>Egg is currently not spawned (Unheld)."));
                    }
                    gui.close(player);
                }
            });
    }

    private GuiItem getAuditLogsItem() {
        List<Component> lore = new ArrayList<>();
        lore.add(AUDIT_LOGS_LORE_HEADER);
        lore.add(Component.empty());

        List<TransitionLog> logs = plugin.getEggAuditService().getCachedLogs();
        if (logs.isEmpty()) {
            lore.add(AUDIT_LOGS_EMPTY);
        } else {
            for (TransitionLog log : logs) {
                String timeStr = TIME_FORMAT.format(java.time.Instant.ofEpochMilli(log.loggedAt()));
                String playerPart = "";
                if (log.playerUuid() != null) {
                    String name = Bukkit.getOfflinePlayer(log.playerUuid()).getName();
                    playerPart = " by <gold>" + (name != null ? name : log.playerUuid().toString().substring(0, 8)) + "</gold>";
                }
                String logLine = String.format("<dark_gray>[%s]</dark_gray> <yellow>%s%s</yellow> at <gray>%s (%d, %d, %d)</gray>",
                    timeStr,
                    log.actionType(),
                    playerPart,
                    log.worldName(),
                    (int)log.x(),
                    (int)log.y(),
                    (int)log.z()
                );
                lore.add(MM.deserialize(logLine));
            }
        }

        return PaperItemBuilder.from(Material.BOOK)
            .name(AUDIT_LOGS_NAME)
            .lore(lore)
            .asGuiItem();
    }

    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null) return "unknown";
        return loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
