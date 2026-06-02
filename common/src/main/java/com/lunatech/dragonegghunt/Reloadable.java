package com.lunatech.dragonegghunt;

/**
 * Implemented in classes that should support being reloaded IE executing the methods during runtime after startup.
 */
public interface Reloadable {
    /**
     * On plugin load.
     */
    default void onLoad(AbstractPlugin plugin) {
    }

    /**
     * On plugin enable.
     */
    default void onEnable(AbstractPlugin plugin) {
    }

    /**
     * On plugin disable.
     */
    default void onDisable(AbstractPlugin plugin) {
    }

}
