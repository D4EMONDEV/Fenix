package fr.d4emon.fenix.loader.metadata;

import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.api.VersionRange;

import java.util.Objects;

/**
 * One entry of a mod's {@code depends}.
 *
 * <p>A dependency does two jobs: it refuses to start when something required is
 * missing or too old, and it orders initialisation so a mod always runs after
 * what it depends on.
 *
 * @param id    the required mod id; {@code minecraft} and {@code fenix} are valid
 * @param range the accepted versions
 */
public record ModDependency(String id, VersionRange range) {

    /**
     * Checks that both parts of the dependency are present.
     *
     * @throws NullPointerException if the id or the range is {@code null}
     */
    public ModDependency {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(range, "range");
    }

    /**
     * Checks a candidate version against this dependency.
     *
     * @param version the version that is actually present
     * @return whether it satisfies the constraint
     */
    public boolean isSatisfiedBy(Version version) {
        return range.contains(version);
    }

    @Override
    public String toString() {
        return id + " " + range;
    }
}
