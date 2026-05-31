package com.lunatech.dragonegghunt.service;

import org.bukkit.inventory.ItemStack;

public interface TrackerRecipeService {
    /**
     * Registers the custom shaped recipe to the server.
     */
    void registerRecipe();

    /**
     * Unregisters the custom shaped recipe from the server.
     */
    void unregisterRecipe();

    /**
     * Checks if the given item stack is a valid tracker compass.
     *
     * @param item the item stack to check
     * @return true if it is a tracker compass, false otherwise
     */
    boolean isTrackerCompass(ItemStack item);

    /**
     * Creates a new tracker compass item stack.
     *
     * @return the tracker compass item
     */
    ItemStack createTrackerCompass();
}
