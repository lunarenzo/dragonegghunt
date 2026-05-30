package com.lunatech.dragonegghunt.persistence;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * Immutable record carrying the state of the Dragon Egg.
 */
public record EggStateData(
    String stateType,
    @Nullable UUID holderUuid,
    @Nullable String worldName,
    @Nullable Double x,
    @Nullable Double y,
    @Nullable Double z,
    @Nullable Long since,
    @Nullable UUID entityUuid
) {}
