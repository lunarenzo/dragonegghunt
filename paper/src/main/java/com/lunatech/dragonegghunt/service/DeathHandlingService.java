package com.lunatech.dragonegghunt.service;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Service to handle player death events involving the Alpha Dragon Egg.
 */
public interface DeathHandlingService {

    /**
     * Handles the death of a player who might be holding or carrying the Alpha Dragon Egg.
     *
     * @param event the player death event
     */
    void handleHolderDeath(@NotNull PlayerDeathEvent event);
}
