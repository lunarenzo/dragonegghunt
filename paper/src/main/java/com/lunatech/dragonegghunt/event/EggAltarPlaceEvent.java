package com.lunatech.dragonegghunt.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when the Alpha Dragon Egg is placed on the altar, either manually by a player
 * or automatically via a respawn.
 */
public final class EggAltarPlaceEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull Location location;
    private final @Nullable Player player;
    private final boolean isAutomatic;
    private boolean cancelled = false;

    public EggAltarPlaceEvent(@NotNull Location location, @Nullable Player player, boolean isAutomatic) {
        this.location = location.clone();
        this.player = player;
        this.isAutomatic = isAutomatic;
    }

    /**
     * Gets the altar location.
     *
     * @return a clone of the altar location
     */
    public @NotNull Location getLocation() {
        return location.clone();
    }

    /**
     * Gets the player who placed the egg, if any.
     *
     * @return the player who placed the egg, or null if placed automatically (e.g. respawn)
     */
    public @Nullable Player getPlayer() {
        return player;
    }

    /**
     * Checks if the placement was triggered automatically by the system.
     *
     * @return true if automatic, false if player-placed
     */
    public boolean isAutomatic() {
        return isAutomatic;
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
