package fr.d4emon.fenix.api;

import java.util.Objects;
import java.util.Optional;

/**
 * A constraint on a {@link Version}, written the way package managers write one.
 *
 * <table border="1">
 * <caption>Accepted syntax</caption>
 * <tr><th>Written</th><th>Means</th></tr>
 * <tr><td>{@code *}</td><td>any version</td></tr>
 * <tr><td>{@code 1.2.3}</td><td>exactly that version</td></tr>
 * <tr><td>{@code >=1.2.0}</td><td>that version or newer; {@code >}, {@code <=} and {@code <} also work</td></tr>
 * <tr><td>{@code ^1.2.0}</td><td>compatible updates, so {@code >=1.2.0 <2.0.0}</td></tr>
 * <tr><td>{@code ~1.2.0}</td><td>patch updates, so {@code >=1.2.0 <1.3.0}</td></tr>
 * </table>
 *
 * <p>Below {@code 1.0.0} the caret tightens, because a project that has not
 * reached its first release breaks things on minor bumps: {@code ^0.2.0} means
 * {@code >=0.2.0 <0.3.0}, and {@code ^0.0.3} means {@code >=0.0.3 <0.0.4}. This
 * matters right now — Fenix itself is a {@code 0.x} project.
 *
 * <p>Bounds are compared exactly as {@link Version} orders versions, so a
 * pre-release falls inside a range that spans it. {@code ^1.0.0} therefore
 * accepts {@code 1.5.0-rc.1}. That is simpler than the exclusion rules package
 * managers layer on top, and predictable, which matters more here.
 *
 * @param lower          the lower bound, or empty if unbounded below
 * @param lowerInclusive whether the lower bound itself is in range
 * @param upper          the upper bound, or empty if unbounded above
 * @param upperInclusive whether the upper bound itself is in range
 */
public record VersionRange(
        Optional<Version> lower, boolean lowerInclusive,
        Optional<Version> upper, boolean upperInclusive) {

    /** Matches every version. */
    public static final VersionRange ANY =
            new VersionRange(Optional.empty(), false, Optional.empty(), false);

    /**
     * Checks that both bounds are supplied. An unbounded side is an empty
     * {@link Optional}, never {@code null}.
     *
     * @throws NullPointerException if either bound is {@code null}
     */
    public VersionRange {
        Objects.requireNonNull(lower, "lower");
        Objects.requireNonNull(upper, "upper");
    }

    /**
     * Parses a constraint.
     *
     * @param text the constraint, for example {@code ^0.1.0} or {@code >=26.2}
     * @return the parsed range
     * @throws IllegalArgumentException if the text is not a constraint
     * @throws NullPointerException     if the text is {@code null}
     */
    public static VersionRange parse(String text) {
        Objects.requireNonNull(text, "text");

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Not a version constraint: '" + text + "'");
        }
        if (trimmed.equals("*")) {
            return ANY;
        }

        // Two-character operators first, or ">=1.0.0" parses as ">" of "=1.0.0".
        if (trimmed.startsWith(">=")) {
            return atLeast(operand(trimmed, 2));
        }
        if (trimmed.startsWith("<=")) {
            return atMost(operand(trimmed, 2));
        }
        if (trimmed.startsWith(">")) {
            return greaterThan(operand(trimmed, 1));
        }
        if (trimmed.startsWith("<")) {
            return lessThan(operand(trimmed, 1));
        }
        if (trimmed.startsWith("^")) {
            return compatibleWith(operand(trimmed, 1));
        }
        if (trimmed.startsWith("~")) {
            return patchesOf(operand(trimmed, 1));
        }
        if (trimmed.startsWith("=")) {
            return exactly(operand(trimmed, 1));
        }
        return exactly(Version.parse(trimmed));
    }

    private static Version operand(String text, int operatorLength) {
        String remainder = text.substring(operatorLength).trim();
        if (remainder.isEmpty()) {
            throw new IllegalArgumentException("Version constraint '" + text + "' has no version after its operator");
        }
        return Version.parse(remainder);
    }

    /**
     * {@return a range matching only the given version}
     *
     * @param version the version to match
     */
    public static VersionRange exactly(Version version) {
        Objects.requireNonNull(version, "version");
        return new VersionRange(Optional.of(version), true, Optional.of(version), true);
    }

    /**
     * {@return a range matching the given version and anything newer}
     *
     * @param version the lowest accepted version
     */
    public static VersionRange atLeast(Version version) {
        return new VersionRange(Optional.of(version), true, Optional.empty(), false);
    }

    /**
     * {@return a range matching anything newer than the given version}
     *
     * @param version the version to exclude, and everything below it
     */
    public static VersionRange greaterThan(Version version) {
        return new VersionRange(Optional.of(version), false, Optional.empty(), false);
    }

    /**
     * {@return a range matching the given version and anything older}
     *
     * @param version the highest accepted version
     */
    public static VersionRange atMost(Version version) {
        return new VersionRange(Optional.empty(), false, Optional.of(version), true);
    }

    /**
     * {@return a range matching anything older than the given version}
     *
     * @param version the version to exclude, and everything above it
     */
    public static VersionRange lessThan(Version version) {
        return new VersionRange(Optional.empty(), false, Optional.of(version), false);
    }

    /**
     * {@return a range of updates that should not break a consumer of the given version}
     *
     * <p>The upper bound follows the leftmost non-zero component, so the range
     * tightens below {@code 1.0.0}. See the type documentation.
     *
     * @param version the version to stay compatible with
     */
    public static VersionRange compatibleWith(Version version) {
        Objects.requireNonNull(version, "version");

        Version upper;
        if (version.major() > 0) {
            upper = new Version(version.major() + 1, 0, 0);
        } else if (version.minor() > 0) {
            upper = new Version(0, version.minor() + 1, 0);
        } else {
            upper = new Version(0, 0, version.patch() + 1);
        }
        return new VersionRange(Optional.of(version), true, Optional.of(upper), false);
    }

    /**
     * {@return a range of patch updates to the given version}
     *
     * @param version the version to stay on the minor line of
     */
    public static VersionRange patchesOf(Version version) {
        Objects.requireNonNull(version, "version");
        Version upper = new Version(version.major(), version.minor() + 1, 0);
        return new VersionRange(Optional.of(version), true, Optional.of(upper), false);
    }

    /**
     * Checks a version against this constraint.
     *
     * @param version the version to test
     * @return whether the version falls in range
     * @throws NullPointerException if the version is {@code null}
     */
    public boolean contains(Version version) {
        Objects.requireNonNull(version, "version");

        if (lower.isPresent()) {
            int comparison = version.compareTo(lower.get());
            if (comparison < 0 || (comparison == 0 && !lowerInclusive)) {
                return false;
            }
        }
        if (upper.isPresent()) {
            int comparison = version.compareTo(upper.get());
            if (comparison > 0 || (comparison == 0 && !upperInclusive)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Renders the range in a canonical form.
     *
     * <p>Deliberately not the text it was parsed from: {@code >=0.2.0 <0.3.0}
     * tells a player what is actually required, where {@code ^0.2.0} assumes
     * they know the notation.
     *
     * @return the range as text
     */
    @Override
    public String toString() {
        if (lower.isEmpty() && upper.isEmpty()) {
            return "*";
        }
        if (lowerInclusive && upperInclusive && lower.equals(upper)) {
            return lower.orElseThrow().toString();
        }

        StringBuilder text = new StringBuilder(24);
        if (lower.isPresent()) {
            text.append(lowerInclusive ? ">=" : ">").append(lower.get());
        }
        if (upper.isPresent()) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(upperInclusive ? "<=" : "<").append(upper.get());
        }
        return text.toString();
    }
}
