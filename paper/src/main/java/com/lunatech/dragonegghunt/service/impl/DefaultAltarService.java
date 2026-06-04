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

        double x = descriptor.x();
        double y = descriptor.y();
        double z = descriptor.z();

        // Dynamically align Y-coordinate if it's the center of the End world exit portal (0, 0)
        if (world.getEnvironment() == World.Environment.THE_END && Math.abs(x) < 1.0 && Math.abs(z) < 1.0) {
            if (world.isChunkLoaded(0, 0) || Bukkit.isPrimaryThread()) {
                if (!world.isChunkLoaded(0, 0)) {
                    world.getChunkAt(0, 0); // Load chunk synchronously
                }
                int highestBedrockY = -1;
                for (int scanY = 50; scanY < 90; scanY++) {
                    if (world.getBlockAt(0, scanY, 0).getType() == Material.BEDROCK) {
                        highestBedrockY = scanY;
                    }
                }
                if (highestBedrockY != -1) {
                    y = highestBedrockY + 1;
                }
            }
        }

        return Optional.of(new Location(world, x, y, z));
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

        // Clear the old egg block / entity / item to prevent duplicates
        clearOldEggState();

        final Location finalLoc = respawnEvent.getLocation();
        
        // Load the chunk if it isn't loaded to prevent glitching/errors
        finalLoc.getChunk().load();

        // Generate the physical pedestal if configured
        generateAltarPedestal();

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
            final var adminActions = plugin.getConfigHandler().getDashboardConfig().adminActions;
            final var descriptor = getAltarLocationDescriptor();
            if (adminActions != null && adminActions.altarWorldNotLoadedMessage != null && !adminActions.altarWorldNotLoadedMessage.isEmpty()) {
                admin.sendMessage(MiniMessage.miniMessage().deserialize(
                    adminActions.altarWorldNotLoadedMessage.replace("{world}", descriptor.worldName())
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

        // Clear the old egg block / entity / item to prevent duplicates
        clearOldEggState();

        final Location finalLoc = respawnEvent.getLocation();
        finalLoc.getChunk().load();

        // Generate the physical pedestal if configured
        generateAltarPedestal();

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
        final var adminActions = plugin.getConfigHandler().getDashboardConfig().adminActions;
        if (adminActions != null && adminActions.respawnBroadcastMessage != null && !adminActions.respawnBroadcastMessage.isEmpty()) {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(adminActions.respawnBroadcastMessage));
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
            plugin.getComponentLogger().info("Skipping altar pedestal generation: generatePedestal is disabled in configuration.");
            return;
        }

        final var resolved = resolveAltarLocation();
        if (resolved.isEmpty()) {
            plugin.getComponentLogger().warn("Cannot generate altar pedestal: Altar world is not loaded.");
            return;
        }

        final String centerBlockName = config.dragonEggTracker.altarLocation.centerBlock;
        Material centerMat = centerBlockName != null ? Material.matchMaterial(centerBlockName) : null;
        if (centerMat == null) {
            centerMat = Material.BEDROCK;
        }
        final String outerBlockName = config.dragonEggTracker.altarLocation.outerBlock;
        Material outerMat = outerBlockName != null ? Material.matchMaterial(outerBlockName) : null;
        if (outerMat == null) {
            outerMat = Material.OBSIDIAN;
        }

        final Location altarLoc = resolved.get();
        final World world = altarLoc.getWorld();
        final int ax = altarLoc.getBlockX();
        final int ay = altarLoc.getBlockY();
        final int az = altarLoc.getBlockZ();

        plugin.getComponentLogger().info("Generating altar pedestal at " + world.getName() + " (" + ax + ", " + (ay - 1) + ", " + az + ") with center block: " + centerMat + ", outer block: " + outerMat);

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

    private void clearOldEggState() {
        final EggState oldState = plugin.getEggTrackerService().getState();
        if (oldState instanceof EggState.Placed placed) {
            final World oldWorld = Bukkit.getWorld(placed.worldName());
            if (oldWorld != null) {
                final Location oldLoc = new Location(oldWorld, placed.x(), placed.y(), placed.z());
                final Block oldBlock = oldLoc.getBlock();
                if (oldBlock.getType() == Material.DRAGON_EGG) {
                    oldBlock.setType(Material.AIR);
                    plugin.getComponentLogger().info("Removed old placed Dragon Egg block at " + placed.worldName() + " (" + placed.x() + ", " + placed.y() + ", " + placed.z() + ") to prevent duplicates.");
                }
            }
        } else if (oldState instanceof EggState.Dropped dropped) {
            final org.bukkit.entity.Entity entity = Bukkit.getEntity(dropped.entityUuid());
            if (entity != null && entity.isValid()) {
                entity.remove();
                plugin.getComponentLogger().info("Removed old dropped Dragon Egg entity to prevent duplicates.");
            }
        } else if (oldState instanceof EggState.Held held) {
            final Player player = Bukkit.getPlayer(held.holderUuid());
            if (player != null && player.isOnline()) {
                // Remove all Alpha Dragon Eggs from the player's inventory
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    final org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
                    if (isAlphaEgg(item)) {
                        player.getInventory().setItem(i, null);
                    }
                }
                // Check offhand
                final org.bukkit.inventory.ItemStack offHand = player.getInventory().getItemInOffHand();
                if (isAlphaEgg(offHand)) {
                    player.getInventory().setItemInOffHand(null);
                }
                // Check cursor
                final org.bukkit.inventory.ItemStack cursor = player.getItemOnCursor();
                if (isAlphaEgg(cursor)) {
                    player.setItemOnCursor(null);
                }
                plugin.getComponentLogger().info("Removed Alpha Dragon Egg item from player " + player.getName() + "'s inventory to prevent duplicates.");
            }
        }
    }

    private boolean isAlphaEgg(@Nullable org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType() != Material.DRAGON_EGG) {
            return false;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER);
    }
}
