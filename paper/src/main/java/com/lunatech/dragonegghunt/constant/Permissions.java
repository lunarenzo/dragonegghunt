package com.lunatech.dragonegghunt.constant;

/**
 * Constants for Dragon Egg Hunt plugin permission nodes.
 * <p>
 * SRP: Constants only. No methods. No logic. No instantiation.
 * ISP: Split by domain (permissions are not mixed with PDC keys or messages).
 */
public final class Permissions {

    private Permissions() {
        throw new UnsupportedOperationException("This is a constant class and cannot be instantiated");
    }

    /**
     * Base permission required to run the main command and view egg info.
     */
    public static final String COMMAND_INFO = "dragonegghunt.command.info";

    /**
     * Permission required to manually set the egg holder.
     */
    public static final String COMMAND_SETHOLDER = "dragonegghunt.command.setholder";

    /**
     * Permission required to toggle the PvP regional override.
     */
    public static final String COMMAND_OVERRIDE = "dragonegghunt.command.override";

    /**
     * Permission required to reset the egg tracker state to Unheld.
     */
    public static final String COMMAND_RESET = "dragonegghunt.command.reset";

    /**
     * Permission required to reload the plugin configuration or components.
     */
    public static final String COMMAND_RELOAD = "dragonegghunt.command.reload";

    /**
     * Permission required to access the admin dashboard GUI.
     */
    public static final String COMMAND_DASHBOARD = "dragonegghunt.command.admin";

    /**
     * Permission required to set the altar location dynamically.
     */
    public static final String COMMAND_SETALTAR = "dragonegghunt.command.setaltar";

    /**
     * Permission required to receive update notifications on join.
     */
    public static final String UPDATE_NOTIFY = "dragonegghunt.update.notify";
}
