package com.lunatech.dragonegghunt.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * The DragonEggHuntAPI class is the main entry point for accessing the DragonEggHunt API.
 */
public abstract class DragonEggHuntAPI {
    private static DragonEggHuntAPI INSTANCE;

    /**
     * Gets the instance of the DragonEggHuntAPI.
     *
     * @return the instance of DragonEggHuntAPI
     * @since 1.0.0
     */
    public static DragonEggHuntAPI getInstance() {
        if (INSTANCE == null)
            throw new RuntimeException("API was accessed before being initialized!");
        return INSTANCE;
    }

    /**
     * Sets the instance of the DragonEggHuntAPI.
     * This method is intended for internal use by the api provider only.
     *
     * @param api the instance of DragonEggHuntAPI to set
     * @since 1.0.0
     */
    @ApiStatus.Internal
    protected static void setInstance(DragonEggHuntAPI api) {
        INSTANCE = api;
    }
}
