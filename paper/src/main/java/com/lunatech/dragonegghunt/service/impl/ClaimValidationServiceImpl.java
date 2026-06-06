package com.lunatech.dragonegghunt.service.impl;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.ClaimValidationService;
import com.lunatech.dragonegghunt.state.EggState;
import io.github.milkdrinkers.wordweaver.Translation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link ClaimValidationService} that verifies the Alpha Dragon Egg placement
 * and handles safe eviction if the egg is located in a protected claim.
 */
public final class ClaimValidationServiceImpl implements ClaimValidationService {

    private final DragonEggHunt plugin;

    /**
     * Instantiates a new Claim validation service.
     *
     * @param plugin the plugin instance
     */
    public ClaimValidationServiceImpl(@NotNull DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public void validateEggLocation() {
        // Only validate if claim placement prevention is enabled
        if (!plugin.getConfigHandler().getConfig().dragonEggTracker.preventClaimPlacement) {
            return;
        }

        // Only validate if the egg is currently placed as a block
        EggState state = plugin.getEggTrackerService().getState();
        if (!(state instanceof EggState.Placed placed)) {
            return;
        }

        World world = Bukkit.getWorld(placed.worldName());
        if (world == null) {
            return;
        }

        Location location = new Location(world, placed.x(), placed.y(), placed.z());

        // Skip validation if the egg is placed at the designated public Altar
        if (plugin.getAltarService().isAtAltar(location)) {
            return;
        }

        // Perform the claim check asynchronously to keep main thread CPU usage at 0ms
        CompletableFuture.supplyAsync(() -> plugin.getClaimProvider().isInClaim(location))
            .thenAccept(isInClaim -> {
                if (isInClaim) {
                    // Eviction must run on the primary server thread to safely modify blocks
                    Bukkit.getScheduler().runTask(plugin, () -> evictEgg(location));
                }
            })
            .exceptionally(ex -> {
                plugin.getSLF4JLogger().error("Failed to perform asynchronous claim verification for egg location", ex);
                return null;
            });
    }

    private void evictEgg(@NotNull Location location) {
        // Re-verify that the egg state is still Placed at this exact location to prevent race conditions
        EggState currentState = plugin.getEggTrackerService().getState();
        if (!(currentState instanceof EggState.Placed placed)) {
            return;
        }

        World world = location.getWorld();
        if (world == null || !placed.worldName().equals(world.getName())
                || (int) Math.floor(placed.x()) != location.getBlockX()
                || (int) Math.floor(placed.y()) != location.getBlockY()
                || (int) Math.floor(placed.z()) != location.getBlockZ()) {
            return;
        }

        // Safely load the chunk if it isn't loaded currently
        org.bukkit.Chunk chunk = location.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }

        // Evict the physical block
        Block block = location.getBlock();
        if (block.getType() == Material.DRAGON_EGG) {
            block.setType(Material.AIR);
        }

        // Recall the egg back to the Altar
        plugin.getAltarService().respawnEggAtAltar("clear");

        // Broadcast eviction to all players
        Bukkit.broadcast(Translation.as("egghunt.place-evicted-claim"));
    }
}
