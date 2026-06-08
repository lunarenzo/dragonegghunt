package com.lunatech.dragonegghunt.service;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;

/**
 * Service to manage active combat sessions between the egg holder and hunters.
 */
public interface CombatSessionService {
    /**
     * Registers or extends a combat session for a hunter attacking the holder.
     *
     * @param hunterUuid the UUID of the attacking hunter
     */
    void registerCombat(@NotNull UUID hunterUuid);

    /**
     * Checks if a hunter has an active combat session with the holder.
     *
     * @param hunterUuid the UUID of the hunter
     * @return true if the session is active
     */
    boolean isInCombat(@NotNull UUID hunterUuid);

    /**
     * Clears all active combat sessions (e.g., when the egg is dropped or holder changes).
     */
    void clearSessions();
}
