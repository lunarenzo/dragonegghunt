package com.lunatech.dragonegghunt.hook;

import org.bukkit.Location;

/**
 * Interface defining the contract for claim/protection checking.
 */
public interface ClaimProvider {
    /**
     * Checks if the given location is inside a claim/protected area.
     *
     * @param location the location to check
     * @return true if the location is claimed/protected, false otherwise
     */
    boolean isInClaim(Location location);
}
