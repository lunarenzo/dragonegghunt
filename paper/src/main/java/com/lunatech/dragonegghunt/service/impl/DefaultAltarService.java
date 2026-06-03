package com.lunatech.dragonegghunt.service.impl;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.event.EggAltarPlaceEvent;
import com.lunatech.dragonegghunt.event.EggAltarRemoveEvent;
import com.lunatech.dragonegghunt.event.EggAltarRespawnEvent;
import com.lunatech.dragonegghunt.service.AltarService;
import com.lunatech.dragonegghunt.state.EggState;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.wordweaver.Translation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Default implementation of {@link AltarService}.
 */
public final class DefaultAltarService implements AltarService {

    private final DragonEggHunt plugin;

    public DefaultAltarService(@NotNull DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull AltarLocationDescriptor getAltarLocationDescriptor() {
        final var altar = plugin.getConfigHandler().getConfig().dragonEggTracker.altarLocation;
        return new AltarLocationDescriptor(altar.world, altar.x, altar.y, altar.z);
    }

    @Override
    public @NotNull Optional<Location> resolveAltarLocation() {
        final var descriptor = getAltarLocationDescriptor();
        final World world = Bukkit.getWorld(descriptor.worldName());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, descriptor.x(), descriptor.y(), descriptor.z()));
    }

    @Override
    public boolean isAltarWorldLoaded() {
        return resolveAltarLocation().isPresent();
    }

    @Override
    public boolean isAtAltar(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return resolveAltarLocation().map(altar -> 
            location.getWorld().getName().equals(altar.getWorld().getName()) &&
            location.getBlockX() == altar.getBlockX() &&
            location.getBlockY() == altar.getBlockY() &&
            location.getBlockZ() == altar.getBlockZ()
        ).orElse(false);
    }

    @Override
    public boolean isAtAltar(@Nullable Block block) {
        if (block == null) {
            return false;
        }
        return isAtAltar(block.getLocation());
    }

    @Override
    public boolean respawnEggAtAltar(@NotNull String reasonKey) {
        // Enforce synchronous execution on the primary thread for block placement and events
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> respawnEggAtAltar(reasonKey));
            return true; // Scheduled successfully
        }

        final Location targetLoc;
        final var resolved = resolveAltarLocation();
        if (resolved.isPresent()) {
            targetLoc = resolved.get();
        } else {
            // Fallback: spawn location of the primary world
            final World primaryWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (primaryWorld != null) {
                targetLoc = primaryWorld.getSpawnLocation();
            } else {
                plugin.getComponentLogger().warn("Failed to respawn egg: No loaded worlds available.");
                return false;
            }
        }

        // Fire custom EggAltarRespawnEvent
        final var respawnEvent = new EggAltarRespawnEvent(targetLoc, reasonKey, null);
        Bukkit.getPluginManager().callEvent(respawnEvent);
        if (respawnEvent.isCancelled()) {
            return false;
        }

        final Location finalLoc = respawnEvent.getLocation();
        
        // Load the chunk if it isn't loaded to prevent glitching/errors
        finalLoc.getChunk().load();

        // Place the egg block
        final Block block = finalLoc.getBlock();
        block.setType(Material.DRAGON_EGG);

        // Fire custom EggAltarPlaceEvent
        final var placeEvent = new EggAltarPlaceEvent(finalLoc, null, true);
        Bukkit.getPluginManager().callEvent(placeEvent);

        // Update tracking state
        plugin.getEggTrackerService().updateState(new EggState.Placed(
            finalLoc.getWorld().getName(),
            finalLoc.getX(),
            finalLoc.getY(),
            finalLoc.getZ()
        ));

        // Broadcast localized message
        final String reason = Translation.of("egghunt.reasons." + reasonKey);
        Bukkit.broadcast(
            ColorParser.of(Translation.of("egghunt.phoenix-respawn"))
                .with("reason", reason != null ? reason : reasonKey)
                .build()
        );

        return true;
    }

    @Override
    public boolean forceRespawnEggAtAltar(@NotNull Player admin) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Administrative respawn must be called on the primary thread.");
        }

        final var resolved = resolveAltarLocation();
        if (resolved.isEmpty()) {
            final var dashboardCfg = plugin.getConfigHandler().getDashboardConfig().altarWorldNotLoadedMessage;
            final var descriptor = getAltarLocationDescriptor();
            if (dashboardCfg != null && !dashboardCfg.isEmpty()) {
                admin.sendMessage(MiniMessage.miniMessage().deserialize(
                    dashboardCfg.replace("{world}", descriptor.worldName())
                ));
            }
            return false;
        }

        final Location targetLoc = resolved.get();

        // Fire custom EggAltarRespawnEvent
        final var respawnEvent = new EggAltarRespawnEvent(targetLoc, "admin", admin);
        Bukkit.getPluginManager().callEvent(respawnEvent);
        if (respawnEvent.isCancelled()) {
            return false;
        }

        final Location finalLoc = respawnEvent.getLocation();
        finalLoc.getChunk().load();

        // Place block
        final Block block = finalLoc.getBlock();
        block.setType(Material.DRAGON_EGG);

        // Fire place event
        final var placeEvent = new EggAltarPlaceEvent(finalLoc, admin, true);
        Bukkit.getPluginManager().callEvent(placeEvent);

        // Update tracking state
        plugin.getEggTrackerService().updateState(new EggState.Placed(
            finalLoc.getWorld().getName(),
            finalLoc.getX(),
            finalLoc.getY(),
            finalLoc.getZ()
        ));

        // Broadcast administrative message
        final var dashboardCfg = plugin.getConfigHandler().getDashboardConfig();
        if (dashboardCfg.respawnBroadcastMessage != null && !dashboardCfg.respawnBroadcastMessage.isEmpty()) {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(dashboardCfg.respawnBroadcastMessage));
        }

        return true;
    }

    @Override
    public boolean isAltarProtectedBlock(@Nullable Block block) {
        if (block == null) {
            return false;
        }
        final var resolved = resolveAltarLocation();
        if (resolved.isEmpty()) {
            return false;
        }

        final Location altarLoc = resolved.get();
        final int ax = altarLoc.getBlockX();
        final int ay = altarLoc.getBlockY();
        final int az = altarLoc.getBlockZ();

        final int bx = block.getX();
        final int by = block.getY();
        final int bz = block.getZ();

        // Check if block is in the Y-1 layer beneath the altar location
        if (by == ay - 1) {
            final int dx = bx - ax;
            final int dz = bz - az;
            // Matches 3x3 platform surrounding the block beneath the altar
            return Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
        }
        return false;
    }

    @Override
    public void generateAltarPedestal() {
        // Must be on primary thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::generateAltarPedestal);
            return;
        }

        final var config = plugin.getConfigHandler().getConfig();
        if (!config.dragonEggTracker.altarLocation.generatePedestal) {
            return;
        }

        final var resolved = resolveAltarLocation();
        if (resolved.isEmpty()) {
            plugin.getComponentLogger().warn("Cannot generate altar pedestal: Altar world is not loaded.");
            return;
        }

        Material centerMat = Material.matchMaterial(config.dragonEggTracker.altarLocation.centerBlock);
        if (centerMat == null) {
            centerMat = Material.BEDROCK;
        }
        Material outerMat = Material.matchMaterial(config.dragonEggTracker.altarLocation.outerBlock);
        if (outerMat == null) {
            outerMat = Material.OBSIDIAN;
        }

        final Location altarLoc = resolved.get();
        final World world = altarLoc.getWorld();
        final int ax = altarLoc.getBlockX();
        final int ay = altarLoc.getBlockY();
        final int az = altarLoc.getBlockZ();

        // Preload target chunk
        altarLoc.getChunk().load();

        // Set Y-1 block underneath center
        world.getBlockAt(ax, ay - 1, az).setType(centerMat);

        // Set surrounding Y-1 blocks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Skip center block
                world.getBlockAt(ax + dx, ay - 1, az + dz).setType(outerMat);
            }
        }
    }

    @Override
    public void updateAltarLocation(@NotNull Location location) {
        final var config = plugin.getConfigHandler().getConfig();
        updateAltarLocation(location, config.dragonEggTracker.altarLocation.generatePedestal);
    }

    @Override
    public void updateAltarLocation(@NotNull Location location, boolean generatePedestal) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> updateAltarLocation(location, generatePedestal));
            return;
        }

        final var config = plugin.getConfigHandler().getConfig();
        config.dragonEggTracker.altarLocation.world = location.getWorld().getName();
        config.dragonEggTracker.altarLocation.x = location.getBlockX() + 0.5;
        config.dragonEggTracker.altarLocation.y = location.getBlockY();
        config.dragonEggTracker.altarLocation.z = location.getBlockZ() + 0.5;
        config.dragonEggTracker.altarLocation.generatePedestal = generatePedestal;

        plugin.getConfigHandler().saveConfig();
        
        if (generatePedestal) {
            generateAltarPedestal();
        }

        plugin.getComponentLogger().info("Altar location dynamically updated to: " 
            + location.getWorld().getName() + " at " 
            + (location.getBlockX() + 0.5) + ", " + location.getBlockY() + ", " + (location.getBlockZ() + 0.5)
            + " (generatePedestal: " + generatePedestal + ")");
    }
}
