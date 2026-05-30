package io.github.exampleuser.example.persistence;

import org.jetbrains.annotations.NotNull;
import java.util.Optional;

/**
 * Interface for reading the Dragon Egg state from persistence.
 */
public interface EggStateReader {
    @NotNull Optional<EggStateData> load();
}
