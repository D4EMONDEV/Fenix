package fr.d4emon.fenix.loader.metadata;

import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.api.Version;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything a {@code fenix.mod.json} declares.
 *
 * <p>This is the loader's own view. Mods see {@link ModInfo}, which is a subset:
 * dependency constraints, mixin configurations and the declared side are of no
 * use to another mod, and keeping them out means loading can be reworked without
 * breaking anyone.
 *
 * @param id          the unique id
 * @param version     the mod's version
 * @param name        the human-readable name; defaults to the id
 * @param description a short description, or an empty string
 * @param authors     the authors, possibly empty
 * @param license     an SPDX identifier, or an empty string
 * @param contact     free-form links, such as {@code homepage} and {@code issues}
 * @param side        where the mod is allowed to run
 * @param depends     what has to be present, and in what versions
 * @param breaks      what this mod refuses to run alongside
 * @param after       what this mod loads after, when present, without needing it
 * @param accessible what the mod asked to reach inside the game
 * @param mixins      mixin configuration files to load from the jar
 */
public record ModMetadata(
        String id,
        Version version,
        String name,
        String description,
        List<String> authors,
        String license,
        Map<String, String> contact,
        ModSide side,
        List<ModDependency> depends,
        List<ModDependency> breaks,
        List<ModDependency> after,
        List<String> mixins,
        List<String> accessible) {

    /**
     * Applies the defaults for every optional field and copies the collections,
     * so nothing read off this record is {@code null} or mutable.
     *
     * @throws NullPointerException if the id or the version is {@code null}
     */
    public ModMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");

        name = name == null || name.isBlank() ? id : name;
        description = description == null ? "" : description;
        authors = authors == null ? List.of() : List.copyOf(authors);
        license = license == null ? "" : license;
        contact = contact == null ? Map.of() : Map.copyOf(contact);
        side = side == null ? ModSide.BOTH : side;
        depends = depends == null ? List.of() : List.copyOf(depends);
        breaks = breaks == null ? List.of() : List.copyOf(breaks);
        after = after == null ? List.of() : List.copyOf(after);
        mixins = mixins == null ? List.of() : List.copyOf(mixins);
        accessible = accessible == null ? List.of() : List.copyOf(accessible);
    }

    /**
     * {@return the subset of this metadata that other mods are allowed to see}
     */
    public ModInfo toModInfo() {
        return new ModInfo(id, version, name, description, authors, license);
    }
}
