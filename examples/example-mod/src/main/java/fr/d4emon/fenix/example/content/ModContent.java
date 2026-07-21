package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.registry.Registrar;

/**
 * The mod's one registrar, shared by {@link ModBlocks} and {@link ModItems}.
 *
 * <p>Keeping it here rather than in either of them means neither has to know
 * about the other, and {@link #register()} is a single call the mod class makes
 * without caring how the content is split up.
 */
public final class ModContent {

    /** Everything this mod adds is namespaced under its id. */
    public static final Registrar REGISTRAR = Registrar.of("example-mod");

    private ModContent() {
    }

    /**
     * Registers everything. Called from {@code onRegister}.
     *
     * <p>Touching both classes is what runs their field initialisers — content
     * declared in a class nobody loads is content that never appears.
     */
    public static void register() {
        ModBlocks.load();
        ModItems.load();
        REGISTRAR.apply();
    }
}
