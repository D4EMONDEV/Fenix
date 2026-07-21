package fr.d4emon.fenix.ember;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class that describes a mod's generated assets and data.
 *
 * <p>The annotated class must implement {@link EmberGenerator}, be concrete and
 * public, and have a public no-argument constructor — checked while the mod
 * compiles, like {@code @Mod}.
 *
 * <pre>{@code
 * @Generator
 * public final class ModAssets implements EmberGenerator {
 *     @Override
 *     public void collect(Ember ember) {
 *         ember.cubeAll(ModBlocks.RUBY_BLOCK);
 *         ember.name(ModBlocks.RUBY_BLOCK, "Ruby Block");
 *     }
 * }
 * }</pre>
 *
 * <p>Nothing in the build points at this class by name: the annotation
 * processor records it, exactly as it records the mod's entry point.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Generator {
}
