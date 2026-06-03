package com.lunatech.dragonegghunt.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when the Alpha Dragon Egg is removed from the altar, e.g. when broken by a player,
 * picked up, or otherwise moved.
 */
public final class EggAltarRemoveEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull Location location;
    private final @Nullable Player player;
    private boolean cancelled = false;

    public EggAltarRemoveEvent(@NotNull Location location, @Nullable Player player) {
        this.location = location.clone();
        this.player = player;
    }

    /**
     * Gets the altar location from which the egg was removed.
     *
     * @return a clone of the altar location
     */
    public @NotNull Location getLocation() {
        return location.clone();
    }

    /**
     * Gets the player who removed the egg, if any.
     *
     * @return the player who removed it, or null if removed by environmental factors or admins
     */
    public @Nullable Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
