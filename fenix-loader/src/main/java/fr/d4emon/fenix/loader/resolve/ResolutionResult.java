package fr.d4emon.fenix.loader.resolve;

import fr.d4emon.fenix.loader.discovery.ModCandidate;

import java.util.List;
import java.util.Objects;

/**
 * A successful resolution.
 *
 * @param loadOrder every mod that will load, dependencies always before the
 *                  mods that declare them
 * @param skipped   mods that are present but declare a side this process is not
 *                  on — normal in any shared modpack, so they are reported for
 *                  the log rather than treated as a problem
 */
public record ResolutionResult(List<ModCandidate> loadOrder, List<ModCandidate> skipped) {

    /**
     * Copies both lists so the result cannot change under its reader.
     *
     * @throws NullPointerException if either list is {@code null}
     */
    public ResolutionResult {
        Objects.requireNonNull(loadOrder, "loadOrder");
        Objects.requireNonNull(skipped, "skipped");
        loadOrder = List.copyOf(loadOrder);
        skipped = List.copyOf(skipped);
    }
}
