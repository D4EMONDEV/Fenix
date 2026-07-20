package fr.d4emon.fenix.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the entry point of a mod.
 *
 * <p>The annotated class must implement {@link FenixMod}, be concrete, and have a
 * public no-argument constructor. Fenix checks all three while the mod compiles,
 * so a mistake is a compiler error with a line number rather than a mod that
 * silently never loads.
 *
 * <p>The id must match the {@code id} of the jar's {@code fenix.mod.json}; the
 * loader refuses to start a mod where the two disagree.
 *
 * <pre>{@code
 * @Mod("example-mod")
 * public final class ExampleMod implements FenixMod {
 *
 *     @Override
 *     public void onInit(Fenix fenix) {
 *         fenix.logger().info("Hello from {}", fenix.mod().name());
 *     }
 * }
 * }</pre>
 *
 * <p>Retention is {@link RetentionPolicy#CLASS}: the annotation is read at
 * compile time and the result is written into the jar, so nothing scans for it
 * at startup. It stays in the bytecode only so that tooling can inspect a jar
 * without recompiling it.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Mod {

    /**
     * {@return the id of the mod this class belongs to}
     */
    String value();
}
