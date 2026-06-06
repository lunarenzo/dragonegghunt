package com.lunatech.dragonegghunt.service;

/**
 * Service responsible for validating the location of the placed Alpha Dragon Egg
 * and evicting it if placed inside protected claimed territories.
 */
public interface ClaimValidationService {
    /**
     * Inspects the current egg state. If the egg is placed inside an illegal claim,
     * it evicts the egg block and triggers a Phoenix Respawn.
     */
    void validateEggLocation();
}
