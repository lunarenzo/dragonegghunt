package com.lunatech.dragonegghunt.persistence;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * Immutable record carrying an audit transition log event of the Dragon Egg.
 */
public record TransitionLog(
    @Nullable Integer id,
    String actionType,
    @Nullable UUID playerUuid,
    String worldName,
    double x,
    double y,
    double z,
    long loggedAt
) {}
