package com.lunatech.dragonegghunt.service.impl;

import com.lunatech.dragonegghunt.persistence.EggStateData;
import com.lunatech.dragonegghunt.persistence.EggStateRepository;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.state.EggState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of EggTrackerService.
 */
public class DefaultEggTrackerService implements EggTrackerService {

    private final EggStateRepository repository;
    private final AtomicReference<EggState> stateRef = new AtomicReference<>(new EggState.Unheld());
    private boolean overrideRegionProtection = false;

    private com.lunatech.dragonegghunt.service.EggStateListener stateListener;

    public DefaultEggTrackerService(@NotNull EggStateRepository repository) {
        this.repository = repository;
    }

    @Override
    public @NotNull EggState getState() {
        return stateRef.get();
    }

    @Override
    public void updateState(@NotNull EggState newState) {
        EggState oldState = stateRef.get();
        stateRef.set(newState);
        saveState(); // Save state asynchronously whenever it updates
        if (stateListener != null) {
            stateListener.onStateTransition(oldState, newState);
        }
    }

    @Override
    public void setStateListener(com.lunatech.dragonegghunt.service.EggStateListener listener) {
        this.stateListener = listener;
    }

    @Override
    public boolean isPvpForced(@NotNull UUID playerUuid) {
        EggState current = stateRef.get();
        if (current instanceof EggState.Held held) {
            return held.holderUuid().equals(playerUuid);
        }
        return false;
    }

    @Override
    public void setOverrideRegionProtection(boolean override) {
        this.overrideRegionProtection = override;
    }

    @Override
    public boolean isOverrideRegionProtection() {
        return overrideRegionProtection;
    }

    @Override
    public void loadState() {
        // Run database read synchronously at startup to block plugin enabling until state is loaded
        try {
            Optional<EggStateData> optionalData = repository.load();
            if (optionalData.isPresent()) {
                EggStateData data = optionalData.get();
                switch (data.stateType()) {
                    case "HELD" -> {
                        if (data.holderUuid() != null) {
                            stateRef.set(new EggState.Held(data.holderUuid(), data.since() != null ? data.since() : System.currentTimeMillis()));
                        } else {
                            stateRef.set(new EggState.Unheld());
                        }
                    }
                    case "PLACED" -> {
                        if (data.worldName() != null && data.x() != null && data.y() != null && data.z() != null) {
                            stateRef.set(new EggState.Placed(data.worldName(), data.x(), data.y(), data.z()));
                        } else {
                            stateRef.set(new EggState.Unheld());
                        }
                    }
                    case "DROPPED" -> {
                        if (data.worldName() != null && data.x() != null && data.y() != null && data.z() != null && data.entityUuid() != null) {
                            stateRef.set(new EggState.Dropped(data.worldName(), data.x(), data.y(), data.z(), data.entityUuid()));
                        } else {
                            stateRef.set(new EggState.Unheld());
                        }
                    }
                    default -> stateRef.set(new EggState.Unheld());
                }
            } else {
                stateRef.set(new EggState.Unheld());
            }
        } catch (Exception e) {
            e.printStackTrace();
            stateRef.set(new EggState.Unheld());
        }
    }

    @Override
    public void saveState() {
        EggState current = stateRef.get();
        EggStateData data;

        if (current instanceof EggState.Held held) {
            data = new EggStateData("HELD", held.holderUuid(), null, null, null, null, held.since(), null);
        } else if (current instanceof EggState.Placed placed) {
            data = new EggStateData("PLACED", null, placed.worldName(), placed.x(), placed.y(), placed.z(), null, null);
        } else if (current instanceof EggState.Dropped dropped) {
            data = new EggStateData("DROPPED", null, dropped.worldName(), dropped.x(), dropped.y(), dropped.z(), null, dropped.entityUuid());
        } else {
            data = new EggStateData("UNHELD", null, null, null, null, null, null, null);
        }

        // Save asynchronously to prevent blocking the main server thread
        CompletableFuture.runAsync(() -> repository.save(data));
    }
}
