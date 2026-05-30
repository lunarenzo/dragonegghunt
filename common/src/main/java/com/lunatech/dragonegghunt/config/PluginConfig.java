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
    }
}

