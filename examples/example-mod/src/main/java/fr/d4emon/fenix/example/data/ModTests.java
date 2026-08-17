package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberTestProvider;
import fr.d4emon.fenix.ember.Generator;
import fr.d4emon.fenix.example.test.ModTestFunctions;

/**
 * The files that turn the mod's test functions into tests that run.
 *
 * <p>Each one names a function and the structure it is placed in. The structure
 * is committed under {@code resources} rather than generated, because nothing
 * generates a template from Java — {@code tools/make-test-structure.py} writes
 * this one.
 */
@Generator
public final class ModTests extends EmberTestProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModTests() {
    }

    @Override
    protected void tests() {
        test("ore_drops", ModTestFunctions.ORE_DROPS)
                .structure("example-mod:test_platform")
                // Breaking a block spawns its drop on the next tick, and the
                // item needs a moment to exist where the runner looks for it.
                .maxTicks(60)
                .save();

        test("door_opens_by_hand", ModTestFunctions.DOOR_OPENS_BY_HAND)
                .structure("example-mod:test_platform")
                .save();

        test("block_entity_is_bound", ModTestFunctions.BLOCK_ENTITY_IS_BOUND)
                .structure("example-mod:test_platform")
                .save();
    }
}
