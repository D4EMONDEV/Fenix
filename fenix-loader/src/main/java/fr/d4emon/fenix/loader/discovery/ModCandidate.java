package fr.d4emon.fenix.loader.discovery;

import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.loader.metadata.ModMetadata;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A mod that was found on disk, before anything has decided whether it can
 * actually load.
 *
 * <p>Called a candidate rather than a mod because resolution still gets to
 * reject it: its dependencies may be missing, it may not run on this side, or
 * another jar may claim the same id.
 *
 * @param metadata what its {@code fenix.mod.json} declares
 * @param path     the jar it was read from
 */
public record ModCandidate(ModMetadata metadata, Path path, boolean nested) {

    /**
     * Checks that both parts are present.
     *
     * @throws NullPointerException if the metadata or the path is {@code null}
     */
    public ModCandidate {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(path, "path");
    }

    /**
     * A mod found loose in the mods directory.
     *
     * @param metadata what its manifest said
     * @param path     where it is
     */
    public ModCandidate(ModMetadata metadata, Path path) {
        this(metadata, path, false);
    }

    /**
     * {@return the mod's id}
     */
    public String id() {
        return metadata.id();
    }

    /**
     * {@return the mod's version}
     */
    public Version version() {
        return metadata.version();
    }

    /**
     * {@return the jar's file name, which is what a player recognises}
     */
    public String fileName() {
        return path.getFileName().toString();
    }

    @Override
    public String toString() {
        return id() + " " + version() + " (" + fileName() + ")";
    }
}
