package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.ember.Ember;
import fr.d4emon.fenix.ember.EmberGenerator;
import fr.d4emon.fenix.ember.Generator;

/**
 * The mod's resource files, described rather than hand-written.
 *
 * <p>Run with {@code gradlew ember}. Content is referred to by the very objects
 * {@link ModBlocks} and {@link ModItems} registered, so nothing repeats a name
 * as a string that could quietly drift out of date.
 *
 * <p>Textures are the one thing not generated — they live in
 * {@code src/main/resources/assets/example-mod/textures/}.
 */
@Generator
public final class ModAssets implements EmberGenerator {

    /** Instantiated by Ember from the compile-time index. */
    public ModAssets() {
    }

    @Override
    public void collect(Ember ember) {
        ember.cubeAll(ModBlocks.RUBY_BLOCK, "Ruby Block");
        ember.cubeAll(ModBlocks.GLOWING_RUBY_BLOCK, "Glowing Ruby Block");

        ember.flatItem(ModItems.RUBY, "Ruby");
        ember.flatItem(ModItems.RUBY_HAMMER, "Ruby Hammer");
    }
}
