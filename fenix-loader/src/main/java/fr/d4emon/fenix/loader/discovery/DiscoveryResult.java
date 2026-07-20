package fr.d4emon.fenix.loader.discovery;

import java.util.List;
import java.util.Objects;

/**
 * What a scan of the mods directory turned up.
 *
 * <p>Problems are returned rather than thrown so that one unreadable jar does
 * not hide the other four. The caller reports them together, then decides
 * whether to continue.
 *
 * @param mods     the mods that were read successfully, ordered by file name
 * @param problems one message per jar that could not be read
 */
public record DiscoveryResult(List<ModCandidate> mods, List<String> problems) {

    /**
     * Copies both lists so the result cannot change under its reader.
     *
     * @throws NullPointerException if either list is {@code null}
     */
    public DiscoveryResult {
        Objects.requireNonNull(mods, "mods");
        Objects.requireNonNull(problems, "problems");
        mods = List.copyOf(mods);
        problems = List.copyOf(problems);
    }

    /**
     * {@return whether any jar failed to be read}
     */
    public boolean hasProblems() {
        return !problems.isEmpty();
    }
}
