package com.lunatech.dragonegghunt.utility;

import com.lunatech.dragonegghunt.DragonEggHunt;

import java.util.Map;

public final class WorldUtil {

    private WorldUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Translates the raw world name into a configured display name if one exists.
     *
     * @param plugin    the plugin instance
     * @param worldName the raw world name
     * @return the configured world display name, or the raw name if not found
     */
    public static String getWorldDisplayName(DragonEggHunt plugin, String worldName) {
        if (worldName == null) {
            return "Unknown";
        }
        Map<String, String> mapping = plugin.getConfigHandler().getConfig().dragonEggTracker.worldDisplayNameMapping;
        if (mapping != null && mapping.containsKey(worldName)) {
            return mapping.get(worldName);
        }
        return worldName;
    }
}
