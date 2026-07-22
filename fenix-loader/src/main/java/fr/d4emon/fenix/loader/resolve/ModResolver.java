package fr.d4emon.fenix.loader.resolve;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.loader.discovery.ModCandidate;
import fr.d4emon.fenix.loader.metadata.ModDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns discovered mods into a load order, or explains why it cannot.
 *
 * <p>Resolution runs in four steps, and the first three collect problems
 * instead of stopping, so a single {@link ResolutionException} names everything
 * at once:
 *
 * <ol>
 * <li>Mods declaring a side this process is not on are set aside. That is
 *     normal — a shared modpack carries client-only mods onto the server — so
 *     it is never an error, but a mod <em>depending</em> on one of them is.</li>
 * <li>Ids must be unique, and must not claim one of the built-in ids.</li>
 * <li>Every dependency must be present in a version inside the declared
 *     range.</li>
 * <li>Mods are ordered so dependencies always come first. Ties are broken
 *     alphabetically by id, which makes the order a function of the mod set
 *     alone — the same mods load the same way on every machine, whatever order
 *     the file system listed them in.</li>
 * </ol>
 */
public final class ModResolver {

    private ModResolver() {
    }

    /**
     * Resolves a load order.
     *
     * @param candidates the discovered mods, in any order
     * @param side       the side this process is running on
     * @param builtins   versions that exist without being mods, keyed by the id
     *                   mods use in {@code depends} — typically {@code fenix}
     *                   and {@code minecraft}
     * @return the load order, plus the mods set aside for being on the wrong side
     * @throws ResolutionException  if any mod cannot load, with every reason listed
     * @throws NullPointerException if any argument is {@code null}
     */
    public static ResolutionResult resolve(
            Collection<ModCandidate> candidates, Side side, Map<String, Version> builtins) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(builtins, "builtins");

        List<String> problems = new ArrayList<>();

        // Step 1 — set aside mods that do not run on this side.
        List<ModCandidate> skipped = new ArrayList<>();
        List<ModCandidate> present = new ArrayList<>();
        for (ModCandidate candidate : candidates) {
            (candidate.metadata().side().includes(side) ? present : skipped).add(candidate);
        }

        // Step 2 — one candidate per id. On a clash the first stays, purely so
        // later steps have something to work with; the launch fails regardless.
        Map<String, ModCandidate> active = new LinkedHashMap<>();
        for (ModCandidate candidate : present) {
            if (builtins.containsKey(candidate.id())) {
                problems.add(candidate.fileName() + ": '" + candidate.id()
                        + "' is a built-in id and cannot be used by a mod");
                continue;
            }
            ModCandidate first = active.putIfAbsent(candidate.id(), candidate);
            if (first == null) {
                continue;
            }

            // Two mods carrying the same library inside them is ordinary, not a
            // mistake: neither author chose it and neither can fix it. Keeping
            // the newer of the two is what makes bundling usable at all —
            // refusing would mean any two mods sharing a dependency could not
            // be installed together.
            //
            // Two loose jars is the other thing entirely: somebody put both
            // files in the folder, and only they can say which one they meant.
            if (first.nested() || candidate.nested()) {
                if (candidate.version().compareTo(first.version()) > 0) {
                    active.put(candidate.id(), candidate);
                }
                continue;
            }

            problems.add("duplicate mod '" + candidate.id() + "': both "
                    + first.fileName() + " and " + candidate.fileName() + " provide it"
                    + " — remove one of them");
        }

        Map<String, ModCandidate> onWrongSide = new HashMap<>();
        for (ModCandidate candidate : skipped) {
            onWrongSide.putIfAbsent(candidate.id(), candidate);
        }

        // Step 3 — every dependency present, in an accepted version.
        for (ModCandidate candidate : active.values()) {
            for (ModDependency dependency : candidate.metadata().depends()) {
                checkDependency(candidate, dependency, active, builtins, onWrongSide, side, problems);
            }
        }

        // Step 3b — nothing present that a mod refuses to run alongside.
        for (ModCandidate candidate : active.values()) {
            for (ModDependency broken : candidate.metadata().breaks()) {
                checkBreak(candidate, broken, active, builtins, problems);
            }
        }

        if (!problems.isEmpty()) {
            throw new ResolutionException(problems);
        }

        // Step 4 — dependencies first, alphabetical among the unconstrained.
        List<ModCandidate> loadOrder = sort(active);
        return new ResolutionResult(loadOrder, skipped);
    }

    private static void checkDependency(
            ModCandidate candidate,
            ModDependency dependency,
            Map<String, ModCandidate> active,
            Map<String, Version> builtins,
            Map<String, ModCandidate> onWrongSide,
            Side side,
            List<String> problems) {

        Version found = builtins.get(dependency.id());
        if (found == null) {
            ModCandidate dependencyMod = active.get(dependency.id());
            if (dependencyMod == null) {
                ModCandidate wrongSide = onWrongSide.get(dependency.id());
                if (wrongSide != null) {
                    problems.add(candidate + " requires " + dependency.id() + ", but " + wrongSide
                            + " only loads on the " + wrongSide.metadata().side().toString().toLowerCase(Locale.ROOT)
                            + " and this is the " + side.toString().toLowerCase(Locale.ROOT));
                } else {
                    problems.add(candidate + " requires " + dependency + ", which is not installed");
                }
                return;
            }
            found = dependencyMod.version();
        }

        if (!dependency.isSatisfiedBy(found)) {
            problems.add(candidate + " requires " + dependency + ", but "
                    + dependency.id() + " " + found + " is present");
        }
    }

    /**
     * Refuses a launch where something a mod cannot work with is present.
     *
     * <p>The alternative is a crash somewhere inside one of the two, which
     * names neither and blames whichever happened to be on the stack. A mod
     * author knows what breaks their mod; this is the only place they can say
     * so before it does.
     */
    private static void checkBreak(
            ModCandidate candidate,
            ModDependency broken,
            Map<String, ModCandidate> active,
            Map<String, Version> builtins,
            List<String> problems) {

        Version present = builtins.get(broken.id());
        if (present == null) {
            ModCandidate other = active.get(broken.id());
            if (other == null) {
                // Absent, which is what a `breaks` entry hopes for.
                return;
            }
            present = other.version();
        }

        if (broken.isSatisfiedBy(present)) {
            problems.add(candidate + " cannot run with " + broken.id() + " " + present
                    + ", which it declares as broken (" + broken + ")");
        }
    }

    /**
     * Kahn's algorithm over the dependency edges, popping the alphabetically
     * smallest ready id first so the order is deterministic.
     */
    private static List<ModCandidate> sort(Map<String, ModCandidate> active) {
        // dependents[x] = mods that must wait for x.
        Map<String, List<String>> dependents = new HashMap<>();
        Map<String, Integer> blockers = new HashMap<>();

        for (ModCandidate candidate : active.values()) {
            int count = 0;
            // `after` orders exactly like `depends` and requires nothing, which
            // is the whole point: a compatibility patch has to run after the
            // mod it patches without refusing to load when that mod is absent.
            // Expressing that with `depends` was the only way before, and it
            // turned every optional integration into a hard requirement.
            List<ModDependency> edges = new ArrayList<>(candidate.metadata().depends());
            edges.addAll(candidate.metadata().after());

            Set<String> seen = new HashSet<>();
            for (ModDependency dependency : edges) {
                // Only edges between actual mods order anything; builtins are
                // always there and never move. A mod named by both `depends`
                // and `after` is one edge, not two — counting it twice would
                // leave it blocked forever.
                if (active.containsKey(dependency.id()) && seen.add(dependency.id())) {
                    dependents.computeIfAbsent(dependency.id(), key -> new ArrayList<>()).add(candidate.id());
                    count++;
                }
            }
            blockers.put(candidate.id(), count);
        }

        PriorityQueue<String> ready = new PriorityQueue<>();
        blockers.forEach((id, count) -> {
            if (count == 0) {
                ready.add(id);
            }
        });

        List<ModCandidate> order = new ArrayList<>(active.size());
        while (!ready.isEmpty()) {
            String id = ready.poll();
            order.add(active.get(id));
            for (String dependent : dependents.getOrDefault(id, List.of())) {
                int remaining = blockers.merge(dependent, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (order.size() < active.size()) {
            Set<String> remaining = new TreeSet<>(active.keySet());
            order.forEach(candidate -> remaining.remove(candidate.id()));
            throw new ResolutionException(List.of(
                    "mods depend on each other in a circle: " + describeCycle(active, remaining)));
        }
        return order;
    }

    /**
     * Walks dependency edges among the unsortable mods until one repeats, which
     * is by construction a member of a cycle.
     */
    private static String describeCycle(Map<String, ModCandidate> active, Set<String> remaining) {
        List<String> path = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        String current = remaining.iterator().next();
        while (seen.add(current)) {
            path.add(current);
            current = active.get(current).metadata().depends().stream()
                    .map(ModDependency::id)
                    .filter(remaining::contains)
                    .sorted()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("not actually cyclic"));
        }

        List<String> cycle = path.subList(path.indexOf(current), path.size());
        return String.join(" -> ", cycle) + " -> " + current;
    }
}
