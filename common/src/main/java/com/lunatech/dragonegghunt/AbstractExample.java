package com.lunatech.dragonegghunt;

import com.lunatech.dragonegghunt.config.ConfigHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractExample extends JavaPlugin {
    private static AbstractExample instance;

    /**
     * Gets plugin instance.
     *
     * @return the plugin instance
     */
    public static AbstractExample getInstance() {
        return AbstractExample.instance;
    }

    AbstractExample() {
        AbstractExample.instance = this;
    }

    /**
     * Gets config handler.
     *
     * @return the config handler
     */
    public abstract @NotNull ConfigHandler getConfigHandler();

    /**
     * Gets egg tracker service.
     *
     * @return the egg tracker service
     */
    public abstract @NotNull com.lunatech.dragonegghunt.service.EggTrackerService getEggTrackerService();

    public abstract @NotNull com.lunatech.dragonegghunt.service.EggAuditService getEggAuditService();
}

