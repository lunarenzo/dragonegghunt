package io.github.exampleuser.example.data.repository;

import io.github.exampleuser.example.data.model.EggStateData;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for writing/updating the Dragon Egg state in persistence.
 */
public interface EggStateWriter {
    void save(@NotNull EggStateData data);
}
