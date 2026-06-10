package com.lunatech.dragonegghunt.service;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for scanning and scrubbing illegal (decorative/duplicate) dragon eggs
 * from player inventories, containers, and blocks in the world.
 * <p>
 * SRP: Isolated to purging illegal dragon eggs.
 * ISP: Interfaces separate by clean capabilities.
 * LSP: Clean concrete implementations are substitutable.
 */
public interface EggCleanerService {

    /**
     * Data carrier record that holds the results of a purge sweep.
     * Utilizing modern Java 21+ records.
     */
    record CleanupReport(
        int onlinePlayersScanned,
        int loadedChunksScanned,
        int eggsRemovedFromInventories,
        int eggsRemovedFromEnderChests,
        int blocksRemovedFromWorlds,
        long elapsedTimeMillis
    ) {}

    /**
     * Cleans illegal dragon eggs from the specified inventory.
     *
     * @param inventory the inventory to clean
     * @return the number of illegal dragon eggs removed
     */
    int cleanInventory(@NotNull Inventory inventory);

    /**
     * Performs a JIT cleanup on a player's inventory and ender chest.
     *
     * @param player the player to clean
     * @return the total number of illegal dragon eggs removed
     */
    int cleanPlayer(@NotNull Player player);

    /**
     * Checks if a placed block of type Material.DRAGON_EGG is at the tracked active Placed location.
     * If it is not, it removes the block and prevents item drops.
     *
     * @param block the block to check
     * @return true if the block was determined to be illegal and was removed, false otherwise
     */
    boolean cleanPlacedBlock(@NotNull org.bukkit.block.Block block);

    /**
     * Executes an active purge sweep across all online players and loaded chunks.
     *
     * @return a CompletableFuture containing the CleanupReport with detailed metrics about the sweep
     */
    @NotNull CompletableFuture<CleanupReport> runActivePurge();
}
