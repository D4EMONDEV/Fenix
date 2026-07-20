package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.loader.metadata.ModMetadata;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A mod that made it all the way in: resolved, on the classpath, instantiated.
 *
 * @param metadata what its {@code fenix.mod.json} declares
 * @param path     the jar it came from
 * @param entries  its instantiated {@code @Mod} classes — empty for a mod that
 *                 ships only resources
 */
public record LoadedMod(ModMetadata metadata, Path path, List<FenixMod> entries) {

    /**
     * Checks the parts and copies the entry list.
     *
     * @throws NullPointerException if any component is {@code null}
     */
    public LoadedMod {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(entries, "entries");
        entries = List.copyOf(entries);
    }

    /**
     * {@return the mod's id}
     */
    public String id() {
        return metadata.id();
    }
}
