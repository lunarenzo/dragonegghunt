package com.lunatech.dragonegghunt.state;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface representing the current runtime state of the Dragon Egg.
 */
public sealed interface EggState {

    record Held(@NotNull UUID holderUuid, long since) implements EggState {}

    record Placed(@NotNull String worldName, double x, double y, double z) implements EggState {}

    record Unheld() implements EggState {}
}
