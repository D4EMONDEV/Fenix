package fr.d4emon.fenix.ember;

import net.minecraft.resources.Identifier;

/**
 * Writes test instances — the files that say which game tests exist, where each
 * one is set up, and how long it is allowed.
 *
 * <p>A game test is two halves that live apart. The Java half is a function
 * registered with {@code Registrar.testFunction}; this is the other half, and
 * without it the function is code nothing ever calls. That failure is the quiet
 * one: the runner reports the tests it found, all of them pass, and the test
 * that was forgotten is simply not in the count.
 *
 * <pre>{@code
 * @Generator
 * public final class ModTests extends EmberTestProvider {
 *
 *     @Override
 *     protected void tests() {
 *         test("ore_drops", ModTestFunctions.ORE_DROPS)
 *                 .structure("example-mod:empty_3x3")
 *                 .maxTicks(100)
 *                 .save();
 *     }
 * }
 * }</pre>
 *
 * <p>Run them with {@code ./gradlew :your-mod:runGameTest}, which starts a
 * headless server, places each structure, runs each function and writes a
 * JUnit report.
 */
public abstract class EmberTestProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberTestProvider() {
    }

    /** Describes the tests. */
    protected abstract void tests();

    @Override
    protected final void run() {
        tests();
    }

    /**
     * Starts a test instance naming a registered function.
     *
     * @param name     the path part of its id, which is what {@code /test run}
     *                 names and what the report calls it
     * @param function the id {@code Registrar.testFunction} returned
     * @return a builder; call {@code save()} when done
     */
    protected final Builder test(String name, Identifier function) {
        return new Builder(this, name, function.toString());
    }

    /**
     * Starts a test instance naming a function by id.
     *
     * @param name     the path part of its id
     * @param function the function's full id
     * @return a builder; call {@code save()} when done
     */
    protected final Builder test(String name, String function) {
        return new Builder(this, name, function);
    }

    /** Collects one test instance. */
    public static final class Builder {

        private final EmberTestProvider provider;
        private final String name;
        private final String function;

        private String structure;
        private String environment = "minecraft:default";
        private int maxTicks = 100;
        private int setupTicks;
        private boolean required = true;
        private boolean manualOnly;
        private int maxAttempts = 1;
        private int requiredSuccesses = 1;
        private String rotation = "none";
        private boolean skyAccess;

        private Builder(EmberTestProvider provider, String name, String function) {
            this.provider = provider;
            this.name = name;
            this.function = function;
        }

        /**
         * The template placed before the function runs.
         *
         * <p>Required, and it must be a {@code .nbt} the mod ships under
         * {@code data/&lt;mod&gt;/structure/}. Even a test that builds
         * everything it needs starts from one, because the structure is also
         * what gives the test its floor and its bounds.
         *
         * @param id the structure's id
         * @return this builder
         */
        public Builder structure(String id) {
            this.structure = id;
            return this;
        }

        /**
         * The environment the test runs in: time of day, weather, difficulty,
         * game rules.
         *
         * <p>Defaults to {@code minecraft:default}. Tests sharing an
         * environment are batched, and the environment is set up once for the
         * batch rather than once per test.
         *
         * @param id the environment's id
         * @return this builder
         */
        public Builder environment(String id) {
            this.environment = id;
            return this;
        }

        /**
         * How long the test may take before it is failed as timed out.
         *
         * @param ticks a tick count; 20 to the second
         * @return this builder
         */
        public Builder maxTicks(int ticks) {
            this.maxTicks = ticks;
            return this;
        }

        /**
         * Ticks to let the world settle after the structure is placed and
         * before the function runs.
         *
         * <p>Worth setting when the test involves anything that needs a tick to
         * exist — a block entity, water finding its level, an entity landing.
         *
         * @param ticks a tick count
         * @return this builder
         */
        public Builder setupTicks(int ticks) {
            this.setupTicks = ticks;
            return this;
        }

        /**
         * Runs only when named, never as part of a full run.
         *
         * @return this builder
         */
        public Builder manualOnly() {
            this.manualOnly = true;
            return this;
        }

        /**
         * Lets a test fail and be retried, passing if enough attempts succeed.
         *
         * <p>For tests that are genuinely random — a drop with a chance on it,
         * a mob choosing where to walk. Using it to paper over a test that
         * fails for a reason turns a red build into a slow one.
         *
         * @param attempts  how many times it may run
         * @param successes how many must pass
         * @return this builder
         */
        public Builder attempts(int attempts, int successes) {
            this.maxAttempts = attempts;
            this.requiredSuccesses = successes;
            return this;
        }

        /**
         * Marks the test as one whose failure does not fail the run.
         *
         * @return this builder
         */
        public Builder optional() {
            this.required = false;
            return this;
        }

        /**
         * Turns the structure before placing it.
         *
         * @param value one of {@code none}, {@code clockwise_90},
         *              {@code 180}, {@code counterclockwise_90}
         * @return this builder
         */
        public Builder rotation(String value) {
            this.rotation = value;
            return this;
        }

        /**
         * Gives the test open sky, for anything that depends on light or
         * weather reaching it.
         *
         * @return this builder
         */
        public Builder skyAccess() {
            this.skyAccess = true;
            return this;
        }

        /** Writes the test instance. */
        public void save() {
            if (structure == null) {
                // The runner needs somewhere to put the test. Without this the
                // file loads and the test fails on placement, which reads as a
                // broken test rather than an unfinished one.
                throw new IllegalStateException(
                        name + " names no structure, so there is nowhere to run it");
            }

            provider.output().data("test_instance/" + name + ".json", """
                    {
                      "type": "minecraft:function",
                      "function": %s,
                      "structure": %s,
                      "environment": %s,
                      "max_ticks": %d,
                      "setup_ticks": %d,
                      "required": %b,
                      "manual_only": %b,
                      "max_attempts": %d,
                      "required_successes": %d,
                      "rotation": %s,
                      "sky_access": %b
                    }
                    """.formatted(EmberOutput.quote(function), EmberOutput.quote(structure),
                    EmberOutput.quote(environment), maxTicks, setupTicks, required,
                    manualOnly, maxAttempts, requiredSuccesses,
                    EmberOutput.quote(rotation), skyAccess));
        }
    }
}
