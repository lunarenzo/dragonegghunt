package io.github.exampleuser.example.service;

import io.github.exampleuser.example.state.EggState;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

/**
 * Service to manage the state of the Dragon Egg Tracker and its PvP rules.
 */
public interface EggTrackerService {

    @NotNull EggState getState();

    void updateState(@NotNull EggState newState);

    boolean isPvpForced(@NotNull UUID playerUuid);

    void setOverrideRegionProtection(boolean override);

    boolean isOverrideRegionProtection();

    void loadState();

    void saveState();
}
