package com.lunatech.dragonegghunt.hook.impl;

import com.lunatech.dragonegghunt.hook.ClaimProvider;
import org.bukkit.Location;

/**
 * Null-object implementation of {@link ClaimProvider} used when no claim/protection plugins are loaded.
 */
public final class NoOpClaimProvider implements ClaimProvider {
    public static final NoOpClaimProvider INSTANCE = new NoOpClaimProvider();

    private NoOpClaimProvider() {}

    @Override
    public boolean isInClaim(Location location) {
        return false;
    }
}
