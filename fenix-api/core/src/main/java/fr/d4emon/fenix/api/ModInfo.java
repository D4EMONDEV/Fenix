package fr.d4emon.fenix.api;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * What one mod tells the rest of the world about itself.
 *
 * <p>This is the public view of a mod, handed to other mods through
 * {@link Fenix#mods()}. It deliberately omits everything only the loader needs —
 * dependency constraints, mixin configurations, the jar it came from — so that
 * loading internals can change without breaking mods.
 *
 * @param id          the unique id, matching the {@link Mod} annotation on the entry point
 * @param version     the mod's version
 * @param name        the human-readable name; defaults to the id when absent
 * @param description a short description, or an empty string
 * @param authors     the authors, possibly empty, never {@code null}
 * @param license     an SPDX identifier, or an empty string
 */
public record ModInfo(
        String id,
        Version version,
        String name,
        String description,
        List<String> authors,
        String license) {

    /**
     * Mod ids are lowercase so they are unambiguous in file paths, resource
     * locations and log output, on file systems that ignore case and those that
     * do not.
     */
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9-]{1,63}");

    /**
     * Validates the id, and turns absent optional values into empty ones so
     * nothing a mod reads off this record can be {@code null}.
     *
     * @throws IllegalArgumentException if the id is not a valid mod id
     * @throws NullPointerException     if the id or the version is {@code null}
     */
    public ModInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");

        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "Not a valid mod id: '" + id + "' (expected 2 to 64 characters, "
                            + "lowercase letters, digits and hyphens, starting with a letter)");
        }

        name = name == null || name.isBlank() ? id : name;
        description = description == null ? "" : description;
        authors = authors == null ? List.of() : List.copyOf(authors);
        license = license == null ? "" : license;
    }

    /**
     * Creates the minimum a mod can describe itself with.
     *
     * @param id      the unique id
     * @param version the mod's version
     */
    public ModInfo(String id, Version version) {
        this(id, version, null, null, null, null);
    }

    /**
     * Checks whether a string is a usable mod id, without throwing.
     *
     * <p>Exposed so that metadata parsing and the annotation processor apply the
     * same rule as this record, and can report a failure in their own terms.
     *
     * @param id the candidate id, possibly {@code null}
     * @return whether the id is valid
     */
    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }
}
