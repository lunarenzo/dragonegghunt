package com.lunatech.dragonegghunt.hook.placeholderapi;

import com.lunatech.dragonegghunt.DragonEggHunt;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A PlaceholderAPI expansion. Read the docs at <a href="https://wiki.placeholderapi.com/developers/creating-a-placeholderexpansion/">here</a> on how to register your custom placeholders.
 */
public class PAPIExpansion extends PlaceholderExpansion {
    private final DragonEggHunt plugin;

    public PAPIExpansion(DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public @NotNull String getIdentifier() {
        return plugin.getPluginMeta().getName().replace(' ', '_').toLowerCase();
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This needs to be true, or PlaceholderAPI will unregister the expansion during a plugin reload.
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer p, @NotNull String params) {
        com.lunatech.dragonegghunt.state.EggState state = plugin.getEggTrackerService().getState();
        return switch (params) {
            case "holder" -> {
                if (state instanceof com.lunatech.dragonegghunt.state.EggState.Held held) {
                    String name = org.bukkit.Bukkit.getOfflinePlayer(held.holderUuid()).getName();
                    yield name != null ? name : "Unknown";
                }
                yield "None";
            }
            case "state" -> {
                if (state instanceof com.lunatech.dragonegghunt.state.EggState.Held) {
                    yield "Held";
                } else if (state instanceof com.lunatech.dragonegghunt.state.EggState.Placed) {
                    yield "Placed";
                } else if (state instanceof com.lunatech.dragonegghunt.state.EggState.Dropped) {
                    yield "Dropped";
                } else {
                    yield "Unheld";
                }
            }
            case "location" -> {
                if (state instanceof com.lunatech.dragonegghunt.state.EggState.Placed placed) {
                    String worldDisp = com.lunatech.dragonegghunt.utility.WorldUtil.getWorldDisplayName(plugin, placed.worldName());
                    yield String.format("%d, %d, %d (%s)", (int) placed.x(), (int) placed.y(), (int) placed.z(), worldDisp);
                } else if (state instanceof com.lunatech.dragonegghunt.state.EggState.Dropped dropped) {
                    String worldDisp = com.lunatech.dragonegghunt.utility.WorldUtil.getWorldDisplayName(plugin, dropped.worldName());
                    yield String.format("%d, %d, %d (%s)", (int) dropped.x(), (int) dropped.y(), (int) dropped.z(), worldDisp);
                }
                yield "unknown";
            }
            default -> null;
        };
    }
}
