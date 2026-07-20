package fr.d4emon.fenix.api;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A semantic version, as defined by <a href="https://semver.org">Semantic Versioning 2.0.0</a>.
 *
 * <p>Parsing is deliberately more permissive than the specification in one way:
 * a missing minor or patch component defaults to zero, so {@code 26.2} parses
 * and equals {@code 26.2.0}. Minecraft numbers its releases that way and there
 * is nothing to gain from refusing them.
 *
 * <p><strong>Ordering ignores build metadata</strong>, as the specification
 * requires, while {@link #equals(Object)} does not. So {@code 1.0.0+a} and
 * {@code 1.0.0+b} compare equal but are not equal — the one place this type
 * knowingly departs from the usual {@link Comparable} advice.
 *
 * @param major      the major component, incremented by incompatible changes
 * @param minor      the minor component, incremented by compatible additions
 * @param patch      the patch component, incremented by compatible fixes
 * @param preRelease the pre-release identifiers, or an empty string for a release
 * @param build      the build metadata, or an empty string when absent
 */
public record Version(int major, int minor, int patch, String preRelease, String build)
        implements Comparable<Version> {

    private static final Pattern PATTERN = Pattern.compile(
            "(0|[1-9]\\d*)"                                     // major
                    + "(?:\\.(0|[1-9]\\d*))?"                   // minor
                    + "(?:\\.(0|[1-9]\\d*))?"                   // patch
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"   // pre-release
                    + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?" // build metadata
    );

    /**
     * Normalises absent pre-release identifiers and build metadata to empty
     * strings, so neither is ever {@code null}.
     *
     * @throws IllegalArgumentException if any component is negative
     */
    public Version {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException(
                    "Version components cannot be negative: " + major + "." + minor + "." + patch);
        }
        preRelease = preRelease == null ? "" : preRelease;
        build = build == null ? "" : build;
    }

    /**
     * Creates a release version with no pre-release identifiers or build metadata.
     *
     * @param major the major component
     * @param minor the minor component
     * @param patch the patch component
     */
    public Version(int major, int minor, int patch) {
        this(major, minor, patch, "", "");
    }

    /**
     * Parses a version.
     *
     * @param text the text to parse, for example {@code 1.2.3}, {@code 26.2} or {@code 0.1.0-SNAPSHOT}
     * @return the parsed version
     * @throws IllegalArgumentException if the text is not a version
     * @throws NullPointerException     if the text is {@code null}
     */
    public static Version parse(String text) {
        Objects.requireNonNull(text, "text");

        Matcher matcher = PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a version: '" + text + "'");
        }

        return new Version(
                Integer.parseInt(matcher.group(1)),
                matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)),
                matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3)),
                matcher.group(4),
                matcher.group(5));
    }

    /**
     * {@return whether this version carries pre-release identifiers}
     *
     * <p>A pre-release always sorts below the release it leads to, so
     * {@code 1.0.0-rc1} is older than {@code 1.0.0}.
     */
    public boolean isPreRelease() {
        return !preRelease.isEmpty();
    }

    @Override
    public int compareTo(Version other) {
        int result = Integer.compare(major, other.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(minor, other.minor);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(patch, other.patch);
        if (result != 0) {
            return result;
        }
        return comparePreRelease(preRelease, other.preRelease);
    }

    private static int comparePreRelease(String left, String right) {
        if (left.equals(right)) {
            return 0;
        }
        // A release outranks any pre-release of the same numbers.
        if (left.isEmpty()) {
            return 1;
        }
        if (right.isEmpty()) {
            return -1;
        }

        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");

        for (int i = 0; i < Math.min(leftParts.length, rightParts.length); i++) {
            int result = compareIdentifier(leftParts[i], rightParts[i]);
            if (result != 0) {
                return result;
            }
        }
        // Everything matched, so the one with more identifiers is the later version.
        return Integer.compare(leftParts.length, rightParts.length);
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = isNumeric(left);
        boolean rightNumeric = isNumeric(right);

        if (leftNumeric && rightNumeric) {
            // Numeric identifiers carry no leading zeros, so the longer one is
            // always the larger one. Comparing that way cannot overflow.
            return left.length() != right.length()
                    ? Integer.compare(left.length(), right.length())
                    : left.compareTo(right);
        }
        // Numeric identifiers always rank below alphanumeric ones.
        if (leftNumeric) {
            return -1;
        }
        if (rightNumeric) {
            return 1;
        }
        return left.compareTo(right);
    }

    private static boolean isNumeric(String identifier) {
        for (int i = 0; i < identifier.length(); i++) {
            if (identifier.charAt(i) < '0' || identifier.charAt(i) > '9') {
                return false;
            }
        }
        return !identifier.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(16)
                .append(major).append('.').append(minor).append('.').append(patch);
        if (!preRelease.isEmpty()) {
            text.append('-').append(preRelease);
        }
        if (!build.isEmpty()) {
            text.append('+').append(build);
        }
        return text.toString();
    }
}
