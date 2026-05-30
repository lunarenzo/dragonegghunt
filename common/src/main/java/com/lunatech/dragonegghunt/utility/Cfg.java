package com.lunatech.dragonegghunt.utility;

import com.lunatech.dragonegghunt.AbstractExample;
import com.lunatech.dragonegghunt.config.ConfigHandler;
import com.lunatech.dragonegghunt.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Convenience class for accessing {@link ConfigHandler#getConfig}
 */
public final class Cfg {
    /**
     * Convenience method for {@link ConfigHandler#getConfig} to getConnection {@link PluginConfig}
     *
     * @return the config
     */
    @NotNull
    public static PluginConfig get() {
        return AbstractExample.getInstance().getConfigHandler().getConfig();
    }
}
