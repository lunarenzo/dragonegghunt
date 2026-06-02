package com.lunatech.dragonegghunt.config;

import com.lunatech.dragonegghunt.config.exception.ConfigValidationException;
import com.lunatech.dragonegghunt.config.migration.Migration;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;
import java.util.Map;

/**
 * Configuration class that maps to dashboard.yml.
 * Defines customize options for the Egg Hunt Admin Dashboard layout, slots, items, and messaging/lores.
 */
@ConfigSerializable
public class DashboardConfig implements VersionedConfig {
    @Comment("Do not change this value!")
    public int configVersion = 1;

    @Override
    @Exclude
    public int configVersion() {
        return configVersion;
    }

    @Override
    @Exclude
    public @NotNull Map<Integer, Migration> migrations() {
        return Map.of();
    }

    @Override
    @Exclude
    public void validate() throws ConfigValidationException {
        if (gui.rows < 1 || gui.rows > 6) {
            throw new ConfigValidationException("gui.rows must be between 1 and 6 inclusive");
        }
        if (gui.eggStatusSlot < 0 || gui.eggStatusSlot >= (gui.rows * 9)) {
            throw new ConfigValidationException("gui.egg-status-slot must be within inventory bounds (0 to " + (gui.rows * 9 - 1) + ")");
        }
        if (gui.adminActionsSlot < 0 || gui.adminActionsSlot >= (gui.rows * 9)) {
            throw new ConfigValidationException("gui.admin-actions-slot must be within inventory bounds (0 to " + (gui.rows * 9 - 1) + ")");
        }
        if (gui.auditLogsSlot < 0 || gui.auditLogsSlot >= (gui.rows * 9)) {
            throw new ConfigValidationException("gui.audit-logs-slot must be within inventory bounds (0 to " + (gui.rows * 9 - 1) + ")");
        }
    }

    @Comment("General GUI Settings")
    public GuiSettings gui = new GuiSettings();

    @Comment("Egg Status Item Settings")
    public EggStatusSettings eggStatus = new EggStatusSettings();

    @Comment("Admin Actions Item Settings")
    public AdminActionsSettings adminActions = new AdminActionsSettings();

    @Comment("Audit Logs Item Settings")
    public AuditLogsSettings auditLogs = new AuditLogsSettings();

    @ConfigSerializable
    public static class GuiSettings {
        @Comment("The title of the Admin Dashboard GUI (MiniMessage format)")
        public String title = "<gold><bold>Dragon Egg Hunt Admin";

        @Comment("Number of rows in the GUI (1 to 6)")
        public int rows = 3;

        @Comment("The slot index for the Egg Status item (0-based)")
        public int eggStatusSlot = 10;

        @Comment("The slot index for the Admin Actions item (0-based)")
        public int adminActionsSlot = 12;

        @Comment("The slot index for the Transition Audit Logs item (0-based)")
        public int auditLogsSlot = 14;

        @Comment("The material used for filling empty background slots")
        public String backgroundMaterial = "GRAY_STAINED_GLASS_PANE";

        @Comment("The display name of the background filler items")
        public String backgroundDisplayName = "";
    }

    @ConfigSerializable
    public static class EggStatusSettings {
        @Comment("Material of the Egg Status item")
        public String material = "DRAGON_EGG";

        @Comment("Display name of the Egg Status item")
        public String displayName = "<light_purple><bold>Alpha Dragon Egg State";

        @Comment("Lore header shown at the top of the item lore")
        public String loreHeader = "<gray>Inspect the current physical state of the egg.";

        @Comment("Lore format when the egg is held by a player. Placeholders: {player}, {status}, {location}")
        public List<String> heldFormat = List.of(
            "<yellow>State: <green>Held by <gold>{player}</gold>",
            "<yellow>Holder: <green>{status}",
            "<yellow>Location: <white>{location}</white>"
        );

        @Comment("Lore format when the egg is placed as a block. Placeholders: {world}, {x}, {y}, {z}")
        public List<String> placedFormat = List.of(
            "<yellow>State: <green>Placed (Block)",
            "<yellow>Location: <white>{world} ({x}, {y}, {z})</white>"
        );

        @Comment("Lore format when the egg is dropped on the ground as an entity. Placeholders: {world}, {x}, {y}, {z}, {location_formatted}")
        public List<String> droppedFormat = List.of(
            "<yellow>State: <green>Dropped (Entity)",
            "<yellow>Location: <white>{location_formatted}</white>"
        );

        @Comment("Lore format when the egg is dropped but in an unloaded chunk or static state. Placeholders: {world}, {x}, {y}, {z}")
        public List<String> droppedUnloadedFormat = List.of(
            "<yellow>State: <green>Dropped (Unloaded / Location)",
            "<yellow>Location: <white>{world} ({x}, {y}, {z})</white>"
        );

        @Comment("Lore format when the egg is unheld or not spawned.")
        public List<String> unheldFormat = List.of(
            "<yellow>State: <red>Unheld / Not Spawned"
        );

        @Comment("Lore line explaining the teleport action (shown only if the egg is locatable)")
        public String teleportActionLine = "<yellow>Left-Click to Teleport to the Egg";

        @Comment("Message sent when the player successfully teleports to the egg")
        public String teleportSuccessMessage = "<green>Teleported to the Alpha Dragon Egg!";

        @Comment("Message sent when teleportation fails because the egg cannot be located")
        public String teleportFailureMessage = "<red>Could not locate the egg to teleport.";
    }

    @ConfigSerializable
    public static class AdminActionsSettings {
        @Comment("Material of the Admin Actions item")
        public String material = "COMMAND_BLOCK";

        @Comment("Display name of the Admin Actions item")
        public String displayName = "<red><bold>Administrative Actions";

        @Comment("Lore header shown at the top of the item lore")
        public String loreHeader = "<gray>Perform manual overrides on the egg hunt event.";

        @Comment("Lore line describing the left-click option")
        public String leftClickDescription = "<yellow>Left-Click: <green>Force Altar Respawn";

        @Comment("Lore line describing the right-click option")
        public String rightClickDescription = "<yellow>Right-Click: <green>Locate & Get Coordinates";

        @Comment("Broadcast message sent when the egg is administratively respawned")
        public String respawnBroadcastMessage = "<light_purple>The Alpha Dragon Egg has been administratively respawned at the altar!";

        @Comment("Error message sent when the altar world is not loaded")
        public String altarWorldNotLoadedMessage = "<red>Altar world '{world}' is not loaded!";

        @Comment("Message sent when located egg is held by a player. Placeholders: {player}")
        public String locateHeldMessage = "<yellow>Egg is currently held by player: <gold>{player}</gold>";

        @Comment("Message sent when located egg is placed. Placeholders: {world}, {x}, {y}, {z}")
        public String locatePlacedMessage = "<yellow>Egg is placed at: <gold>{world} ({x}, {y}, {z})</gold>";

        @Comment("Message sent when located egg is dropped. Placeholders: {world}, {x}, {y}, {z}")
        public String locateDroppedMessage = "<yellow>Egg is dropped at: <gold>{world} ({x}, {y}, {z})</gold>";

        @Comment("Message sent when located egg is not spawned")
        public String locateUnheldMessage = "<red>Egg is currently not spawned (Unheld).";
    }

    @ConfigSerializable
    public static class AuditLogsSettings {
        @Comment("Material of the Audit Logs item")
        public String material = "BOOK";

        @Comment("Display name of the Audit Logs item")
        public String displayName = "<aqua><bold>Recent Egg Transitions";

        @Comment("Lore header shown at the top of the item lore")
        public String loreHeader = "<gray>Real-time lifecycle timeline (last 10 transitions):";

        @Comment("Lore lines shown when there are no logs to display")
        public String emptyMessage = "<red>No logs recorded yet.";

        @Comment("Format of each audit log line in lore. Placeholders: {time}, {action}, {player_info}, {world}, {x}, {y}, {z}")
        public String logLineFormat = "<dark_gray>[{time}]</dark_gray> <yellow>{action}{player_info}</yellow> at <gray>{world} ({x}, {y}, {z})</gray>";

        @Comment("Format of the player portion when a player triggered the action. Placeholders: {player}")
        public String playerInfoFormat = " by <gold>{player}</gold>";
    }
}
