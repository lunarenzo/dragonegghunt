package com.lunatech.dragonegghunt.service;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Service to manage the Alpha Dragon Egg Altar.
 * Exposes methods to validate locations, trigger respawns safely with event handling,
 * protect the altar block pedestal, and dynamically generate it.
 */
public interface AltarService {

    /**
     * Represents a coordinate location for an Altar.
     * Replaces raw values with a modern, immutable record.
     */
    record AltarLocationDescriptor(
        @NotNull String worldName,
        double x,
        double y,
        double z
    ) {
        /**
         * Creates a descriptor from a Bukkit Location.
         *
         * @param loc the Bukkit location
         * @return the descriptor
         */
        public static AltarLocationDescriptor fromLocation(@NotNull Location loc) {
            return new AltarLocationDescriptor(
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                loc.getX(),
                loc.getY(),
                loc.getZ()
            );
        }
    }

    /**
     * Gets the configured Altar location descriptor.
     *
     * @return the altar location descriptor
     */
    @NotNull AltarLocationDescriptor getAltarLocationDescriptor();

    /**
     * Resolves the actual Bukkit Location from the configured Altar location.
     *
     * @return the Bukkit Location, or empty if the world is not loaded
     */
    @NotNull Optional<Location> resolveAltarLocation();

    /**
     * Checks if the world containing the Altar is currently loaded.
     *
     * @return true if the altar world is loaded, false otherwise
     */
    boolean isAltarWorldLoaded();

    /**
     * Checks if the given location matches the configured Altar location.
     *
     * @param location the location to check
     * @return true if matching, false otherwise
     */
    boolean isAtAltar(@Nullable Location location);

    /**
     * Checks if the given block is located at the Altar.
     *
     * @param block the block to check
     * @return true if it is the altar block, false otherwise
     */
    boolean isAtAltar(@Nullable Block block);

    /**
     * Triggers a phoenix respawn for the egg at the altar.
     * Fires the {@link com.lunatech.dragonegghunt.event.EggAltarRespawnEvent} and
     * {@link com.lunatech.dragonegghunt.event.EggAltarPlaceEvent}.
     *
     * @param reasonKey the reason key for the respawn (e.g. lava, void, despawn)
     * @return true if the respawn succeeded and was not cancelled
     */
    boolean respawnEggAtAltar(@NotNull String reasonKey);

    /**
     * Triggers a manual administrative force respawn of the egg at the altar.
     *
     * @param admin the admin player initiating the respawn
     * @return true if successful and not cancelled
     */
    boolean forceRespawnEggAtAltar(@NotNull Player admin);

    /**
     * Checks if the given block is part of the protected Altar pedestal structure.
     *
     * @param block the block to check
     * @return true if the block is protected, false otherwise
     */
    boolean isAltarProtectedBlock(@Nullable Block block);

    /**
     * Generates the physical Altar pedestal structure in the world.
     * By default, generates a durable obsidian/bedrock pedestal at the altar location.
     */
    void generateAltarPedestal();

    /**
     * Updates the Altar coordinates dynamically and saves the new values to disk.
     *
     * @param location the new location of the Altar
     */
    void updateAltarLocation(@NotNull Location location);

    /**
     * Updates the Altar coordinates dynamically, saves the new values to disk, and
     * optionally generates the physical pedestal.
     *
     * @param location         the new location of the Altar
     * @param generatePedestal true to generate the configured pedestal,
     *                         false to preserve the existing block layout
     */
    void updateAltarLocation(@NotNull Location location, boolean generatePedestal);
}
