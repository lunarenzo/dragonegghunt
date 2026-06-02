package com.lunatech.dragonegghunt.utility;


import com.lunatech.dragonegghunt.AbstractPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;

/**
 * A class that provides shorthand access to {@link AbstractPlugin#getComponentLogger}.
 */
public class Logger {
    /**
     * Get component logger. Shorthand for:
     *
     * @return the component logger {@link AbstractPlugin#getComponentLogger}.
     */
    @NotNull
    public static ComponentLogger get() {
        return AbstractPlugin.getInstance().getComponentLogger();
    }
}
