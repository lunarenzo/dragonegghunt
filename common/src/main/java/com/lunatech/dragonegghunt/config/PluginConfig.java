package com.lunatech.dragonegghunt.config;

import com.lunatech.dragonegghunt.config.exception.ConfigValidationException;
import com.lunatech.dragonegghunt.config.migration.Migration;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Map;

@ConfigSerializable
public class PluginConfig implements VersionedConfig {
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
    }

    @Comment("Update Checker Settings")
    public UpdateChecker updateChecker = new UpdateChecker();

    @ConfigSerializable
    public static class UpdateChecker {
        @Comment("Should the plugin check for plugin updates on startup?")
        public boolean enabled = true;

        @Comment("Send update notifications to the console?")
        public boolean console = true;

        @Comment("Send update notifications to opped players on join?")
        public boolean op = true;
    }

    @Comment("Language, specify the language file to use, for example `en_US` which will load `/lang/en_US.json`")
    public String language = "en_US";

    @Comment("Dragon Egg Tracker Settings")
    public DragonEggTracker dragonEggTracker = new DragonEggTracker();

    @ConfigSerializable
    public static class DragonEggTracker {
        @Comment("Broadcast interval in ticks (e.g. 60 ticks = 3 seconds)")
        public int broadcastInterval = 60;

        @Comment("Should PvP bypass WorldGuard and other regional protections?")
        public boolean overrideRegionProtection = false;

        @Comment("Should the Alpha Dragon Egg be allowed to be stored inside containers (chests, hoppers, dispensers, etc.)?")
        public boolean allowContainerStorage = false;

        @Comment("The altar location where the Alpha Egg will respawn if destroyed or despawned")
        public AltarLocation altarLocation = new AltarLocation();

        @Comment("Tracking feedback method: COMPASS, ACTIONBAR, BOTH, or NONE")
        public String trackingMethod = "BOTH";

        @Comment("Should the ActionBar message be shown ONLY to players holding the tracker compass? (If false, it is broadcast to everyone if actionbar is enabled)")
        public boolean actionbarOnlyForHolders = false;

        @Comment("Display name of the Alpha Dragon Egg item")
        public String eggDisplayName = "<light_purple><bold>Alpha Dragon Egg";

        @Comment("Lore of the Alpha Dragon Egg item")
        public java.util.List<String> eggLore = java.util.List.of(
            "<gray>The legendary egg being hunted.",
            "<red>Keep it safe!"
        );

        @Comment("Custom world display names (mappings from raw world folder name to custom name)")
        public java.util.Map<String, String> worldDisplayNameMapping = java.util.Map.of(
            "world", "Overworld",
            "world_nether", "The Nether",
            "world_the_end", "The End"
        );

        @Comment("Compass tracker item settings")
        public CompassTracker compassTracker = new CompassTracker();

        @Comment("Potion buff reward settings for the holder of the Alpha Dragon Egg")
        public PotionBuffReward potionBuffReward = new PotionBuffReward();
    }

    @ConfigSerializable
    public static class PotionBuffReward {
        @Comment("Should players holding the Alpha Dragon Egg receive configured potion effects?")
        public boolean enabled = false;

        @Comment("List of potion effects to apply to the holder. Format: EFFECT_TYPE:AMPLIFIER (e.g. SPEED:1, REGENERATION:0)")
        public java.util.List<String> effects = java.util.List.of(
            "SPEED:0",
            "REGENERATION:0"
        );
    }

    @ConfigSerializable
    public static class CompassTracker {
        @Comment("Is the custom compass tracker item enabled and craftable?")
        public boolean enabled = true;

        @Comment("Custom crafting recipe layout shape (3 rows, spaces represent empty slots)")
        public java.util.List<String> shape = java.util.List.of(
            " E ",
            "ECE",
            " E "
        );

        @Comment("Ingredients mapping for the recipe shape")
        public java.util.Map<String, String> ingredients = java.util.Map.of(
            "E", "ENDER_EYE",
            "C", "COMPASS"
        );

        @Comment("Display name of the custom compass tracker item")
        public String displayName = "<gold>Alpha Egg Tracker";

        @Comment("Lore of the custom compass tracker item")
        public java.util.List<String> lore = java.util.List.of(
            "<gray>Points towards the Alpha Dragon Egg.",
            "<red>Active when held in hand."
        );
    }

    @ConfigSerializable
    public static class AltarLocation {
        @Comment("The world name")
        public String world = "world_the_end";

        @Comment("The X coordinate")
        public double x = 0.5;

        @Comment("The Y coordinate")
        public double y = 68.0;

        @Comment("The Z coordinate")
        public double z = 0.5;
    }
}

