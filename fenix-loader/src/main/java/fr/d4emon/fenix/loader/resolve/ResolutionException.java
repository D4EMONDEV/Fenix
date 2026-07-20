package fr.d4emon.fenix.loader.resolve;

import java.util.List;

/**
 * Thrown when the discovered mods cannot be turned into a load order.
 *
 * <p>Resolution never gives up at the first problem: it checks everything, then
 * throws once with the complete list. A player fixing a modpack gets one
 * message naming every missing dependency, instead of a launch-crash loop that
 * reveals them one at a time.
 */
public final class ResolutionException extends RuntimeException {

    private final transient List<String> problems;

    /**
     * Creates the exception from everything that went wrong.
     *
     * @param problems one entry per problem, in the order they were found
     * @throws IllegalArgumentException if the list is empty
     */
    public ResolutionException(List<String> problems) {
        super(buildMessage(problems));
        this.problems = List.copyOf(problems);
    }

    /**
     * {@return every problem, one entry each}
     */
    public List<String> problems() {
        return problems;
    }

    private static String buildMessage(List<String> problems) {
        if (problems.isEmpty()) {
            throw new IllegalArgumentException("a ResolutionException needs at least one problem");
        }

        StringBuilder message = new StringBuilder(64 + problems.size() * 64);
        message.append(problems.size() == 1
                ? "Fenix cannot start because of a mod problem:"
                : "Fenix cannot start because of " + problems.size() + " mod problems:");
        for (String problem : problems) {
            message.append(System.lineSeparator()).append("  - ").append(problem);
        }
        return message.toString();
    }
}
