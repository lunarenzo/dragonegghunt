package com.lunatech.dragonegghunt.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when the Alpha Dragon Egg is about to be respawned at the altar.
 * This event is cancellable.
 */
public final class EggAltarRespawnEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final @NotNull Location location;
    private final @NotNull String reasonKey;
    private final @Nullable Player initiator;
    private boolean cancelled = false;

    public EggAltarRespawnEvent(@NotNull Location location, @NotNull String reasonKey, @Nullable Player initiator) {
        this.location = location.clone();
        this.reasonKey = reasonKey;
        this.initiator = initiator;
    }

    /**
     * Gets the target location of the altar.
     *
     * @return a clone of the altar location
     */
    public @NotNull Location getLocation() {
        return location.clone();
    }

    /**
     * Gets the reason key for the respawn.
     *
     * @return the reason key
     */
    public @NotNull String getReasonKey() {
        return reasonKey;
    }

    /**
     * Gets the player who initiated the respawn (e.g. an admin via dashboard).
     *
     * @return the initiating player, or null if triggered automatically
     */
    public @Nullable Player getInitiator() {
        return initiator;
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
