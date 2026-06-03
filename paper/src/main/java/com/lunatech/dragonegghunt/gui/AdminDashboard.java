package com.lunatech.dragonegghunt.gui;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.config.DashboardConfig;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Modern inventory-based administration dashboard UI built using TriumphGUI.
 * Layout, labels, and behavior are loaded dynamically from DashboardConfig.
 */
public final class AdminDashboard {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final java.time.format.DateTimeFormatter TIME_FORMAT = 
        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault());

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
        final DashboardConfig dashCfg = plugin.getConfigHandler().getDashboardConfig();
        final DashboardConfig.GuiSettings guiCfg = dashCfg.gui;

        // Custom Title component (GUI Titles are not italicized by default in Minecraft)
        final Component titleComponent = MM.deserialize(guiCfg.title);

        // Create GUI
        final Gui gui = Gui.gui()
            .title(titleComponent)
            .rows(guiCfg.rows)
            .disableAllInteractions()
            .create();

        // Fill background borders
        Material bgMaterial = Material.matchMaterial(guiCfg.backgroundMaterial);
        if (bgMaterial == null) {
            bgMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }
        final Component bgName = guiCfg.backgroundDisplayName.isEmpty() 
            ? Component.empty() 
            : MM.deserialize("<!italic>" + guiCfg.backgroundDisplayName);

        final GuiItem border = PaperItemBuilder.from(bgMaterial)
            .name(bgName)
            .asGuiItem();
        gui.getFiller().fill(border);

        // Egg Status & Location item
        gui.setItem(guiCfg.eggStatusSlot, getEggStatusItem(player, gui, dashCfg));

        // Admin Actions item
        gui.setItem(guiCfg.adminActionsSlot, getAdminActionsItem(player, gui, dashCfg));

        // Transition Audit Logs item
        gui.setItem(guiCfg.auditLogsSlot, getAuditLogsItem(dashCfg));

        gui.open(player);
    }

    private GuiItem getEggStatusItem(Player player, Gui gui, DashboardConfig dashCfg) {
        final DashboardConfig.EggStatusSettings cfg = dashCfg.eggStatus;
        final EggState state = plugin.getEggTrackerService().getState();
        final List<Component> lore = new ArrayList<>();
        org.bukkit.Location targetLoc = null;

        lore.add(MM.deserialize("<!italic>" + cfg.loreHeader));
        lore.add(Component.empty());

        if (state instanceof EggState.Held held) {
            final String name = Bukkit.getOfflinePlayer(held.holderUuid()).getName();
            final String holderName = name != null ? name : "Unknown";
            final Player holder = Bukkit.getPlayer(held.holderUuid());
            final boolean isOnline = holder != null && holder.isOnline();
            final String holderStatus = isOnline ? "Online" : "Offline";
            if (isOnline) {
                targetLoc = holder.getLocation();
            }
            final String locStr = isOnline ? formatLocation(targetLoc) : "unknown";

            for (String line : cfg.heldFormat) {
                lore.add(MM.deserialize("<!italic>" + line
                    .replace("{player}", holderName)
                    .replace("{status}", holderStatus)
                    .replace("{location}", locStr)
                ));
            }
        } else if (state instanceof EggState.Placed placed) {
            final org.bukkit.World world = Bukkit.getWorld(placed.worldName());
            if (world != null) {
                targetLoc = new org.bukkit.Location(world, placed.x(), placed.y(), placed.z());
            }
            for (String line : cfg.placedFormat) {
                lore.add(MM.deserialize("<!italic>" + line
                    .replace("{world}", placed.worldName())
                    .replace("{x}", String.valueOf((int) placed.x()))
                    .replace("{y}", String.valueOf((int) placed.y()))
                    .replace("{z}", String.valueOf((int) placed.z()))
                ));
            }
        } else if (state instanceof EggState.Dropped dropped) {
            final org.bukkit.entity.Entity entity = Bukkit.getEntity(dropped.entityUuid());
            if (entity != null && entity.isValid()) {
                targetLoc = entity.getLocation();
                for (String line : cfg.droppedFormat) {
                    lore.add(MM.deserialize("<!italic>" + line
                        .replace("{world}", targetLoc.getWorld().getName())
                        .replace("{x}", String.valueOf((int) targetLoc.getX()))
                        .replace("{y}", String.valueOf((int) targetLoc.getY()))
                        .replace("{z}", String.valueOf((int) targetLoc.getZ()))
                        .replace("{location_formatted}", formatLocation(targetLoc))
                    ));
                }
            } else {
                for (String line : cfg.droppedUnloadedFormat) {
                    lore.add(MM.deserialize("<!italic>" + line
                        .replace("{world}", dropped.worldName())
                        .replace("{x}", String.valueOf((int) dropped.x()))
                        .replace("{y}", String.valueOf((int) dropped.y()))
                        .replace("{z}", String.valueOf((int) dropped.z()))
                    ));
                }
            }
        } else {
            for (String line : cfg.unheldFormat) {
                lore.add(MM.deserialize("<!italic>" + line));
            }
        }

        if (targetLoc != null && !cfg.teleportActionLine.isEmpty()) {
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic>" + cfg.teleportActionLine));
        }

        final org.bukkit.Location finalLoc = targetLoc;

        Material eggMaterial = Material.matchMaterial(cfg.material);
        if (eggMaterial == null) {
            eggMaterial = Material.DRAGON_EGG;
        }

        return PaperItemBuilder.from(eggMaterial)
            .name(MM.deserialize("<!italic>" + cfg.displayName))
            .lore(lore)
            .asGuiItem(event -> {
                if (finalLoc != null) {
                    player.teleport(finalLoc);
                    if (!cfg.teleportSuccessMessage.isEmpty()) {
                        player.sendMessage(MM.deserialize(cfg.teleportSuccessMessage));
                    }
                    gui.close(player);
                } else {
                    if (!cfg.teleportFailureMessage.isEmpty()) {
                        player.sendMessage(MM.deserialize(cfg.teleportFailureMessage));
                    }
                }
            });
    }

    private GuiItem getAdminActionsItem(Player player, Gui gui, DashboardConfig dashCfg) {
        final DashboardConfig.AdminActionsSettings cfg = dashCfg.adminActions;
        final List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic>" + cfg.loreHeader));
        lore.add(Component.empty());
        if (!cfg.leftClickDescription.isEmpty()) {
            lore.add(MM.deserialize("<!italic>" + cfg.leftClickDescription));
        }
        if (!cfg.rightClickDescription.isEmpty()) {
            lore.add(MM.deserialize("<!italic>" + cfg.rightClickDescription));
        }

        Material actionMaterial = Material.matchMaterial(cfg.material);
        if (actionMaterial == null) {
            actionMaterial = Material.COMMAND_BLOCK;
        }

        return PaperItemBuilder.from(actionMaterial)
            .name(MM.deserialize("<!italic>" + cfg.displayName))
            .lore(lore)
            .asGuiItem(event -> {
                if (event.isLeftClick()) {
                    plugin.getAltarService().forceRespawnEggAtAltar(player);
                    gui.close(player);
                } else if (event.isRightClick()) {
                    final EggState state = plugin.getEggTrackerService().getState();
                    if (state instanceof EggState.Held held) {
                        final String name = Bukkit.getOfflinePlayer(held.holderUuid()).getName();
                        final String holderName = name != null ? name : held.holderUuid().toString();
                        player.sendMessage(MM.deserialize(cfg.locateHeldMessage.replace("{player}", holderName)));
                    } else if (state instanceof EggState.Placed placed) {
                        player.sendMessage(MM.deserialize(cfg.locatePlacedMessage
                            .replace("{world}", placed.worldName())
                            .replace("{x}", String.valueOf((int) placed.x()))
                            .replace("{y}", String.valueOf((int) placed.y()))
                            .replace("{z}", String.valueOf((int) placed.z()))
                        ));
                    } else if (state instanceof EggState.Dropped dropped) {
                        player.sendMessage(MM.deserialize(cfg.locateDroppedMessage
                            .replace("{world}", dropped.worldName())
                            .replace("{x}", String.valueOf((int) dropped.x()))
                            .replace("{y}", String.valueOf((int) dropped.y()))
                            .replace("{z}", String.valueOf((int) dropped.z()))
                        ));
                    } else {
                        player.sendMessage(MM.deserialize(cfg.locateUnheldMessage));
                    }
                    gui.close(player);
                }
            });
    }

    private GuiItem getAuditLogsItem(DashboardConfig dashCfg) {
        final DashboardConfig.AuditLogsSettings cfg = dashCfg.auditLogs;
        final List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<!italic>" + cfg.loreHeader));
        lore.add(Component.empty());

        final List<TransitionLog> logs = plugin.getEggAuditService().getCachedLogs();
        if (logs.isEmpty()) {
            lore.add(MM.deserialize("<!italic>" + cfg.emptyMessage));
        } else {
            for (TransitionLog log : logs) {
                final String timeStr = TIME_FORMAT.format(java.time.Instant.ofEpochMilli(log.loggedAt()));
                String playerPart = "";
                if (log.playerUuid() != null) {
                    final String name = Bukkit.getOfflinePlayer(log.playerUuid()).getName();
                    final String holderName = name != null ? name : log.playerUuid().toString().substring(0, 8);
                    playerPart = cfg.playerInfoFormat.replace("{player}", holderName);
                }
                final String logLine = cfg.logLineFormat
                    .replace("{time}", timeStr)
                    .replace("{action}", log.actionType())
                    .replace("{player_info}", playerPart)
                    .replace("{world}", log.worldName())
                    .replace("{x}", String.valueOf((int) log.x()))
                    .replace("{y}", String.valueOf((int) log.y()))
                    .replace("{z}", String.valueOf((int) log.z()));
                lore.add(MM.deserialize("<!italic>" + logLine));
            }
        }

        Material logsMaterial = Material.matchMaterial(cfg.material);
        if (logsMaterial == null) {
            logsMaterial = Material.BOOK;
        }

        return PaperItemBuilder.from(logsMaterial)
            .name(MM.deserialize("<!italic>" + cfg.displayName))
            .lore(lore)
            .asGuiItem();
    }

    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null) return "unknown";
        return loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
