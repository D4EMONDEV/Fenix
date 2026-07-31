package fr.d4emon.fenix.conformance;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import fr.d4emon.fenix.loader.mixin.MixinSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a mod's fluids can be drawn.
 *
 * <p>The registry conformance check boots the game headless, so it proves a
 * fluid is registered and wired but never bakes a single sprite — the one thing
 * it cannot reach is whether a modded fluid is <em>drawn</em>. That hangs on one
 * injection landing: 26.2 bakes fluid models into a two-entry map of water and
 * lava inside {@code FluidStateModelSet.bake}, and Fenix adds a mod's fluids
 * there. If that injection stops landing — {@code bake} renamed, its shape
 * changed under a game update — every modded fluid silently falls back to the
 * missing-texture model, and nothing says why.
 *
 * <p>So this loads the real class through the loader with the mixin applied, and
 * checks the merged handler is there. Same technique as {@code EntityMixinTest},
 * for the same reason: a rendering table that is vanilla's and not open.
 */
class FluidRenderMixinTest {

    private static final String FLUID_MODELS =
            "net.minecraft.client.renderer.block.FluidStateModelSet";
    private static final String BAKE_MODDED = "fenix$bakeModdedFluids";

    @Test
    @DisplayName("the fluid model injection still lands on Minecraft's fluid model set")
    void fluidRenderMixinAppliesToRealMinecraft() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path registryJar = requiredFile("fenix.test.registryJar");

        Map<String, byte[]> transformed = new ConcurrentHashMap<>();

        try (FenixClassLoader loader = new FenixClassLoader(getClass().getClassLoader())) {
            loader.addPath(clientJar);
            loader.addPath(registryJar);
            MixinSetup.bootstrap(loader, Side.CLIENT, List.of("fenix-api-registry.mixins.json"));
            loader.addTransformer((name, bytes) -> {
                transformed.put(name, bytes);
                return bytes;
            });

            loader.loadClass(FLUID_MODELS);

            byte[] bytes = transformed.get(FLUID_MODELS);
            assertNotNull(bytes, FLUID_MODELS + " was never defined through the Fenix classloader");
            // Mixin renames private handlers as it merges them, so the name it
            // was written under is a substring of what ends up in the class.
            assertTrue(methodNames(bytes).stream().anyMatch(name -> name.contains(BAKE_MODDED)),
                    FLUID_MODELS + " should carry " + BAKE_MODDED
                            + " — without it every modded fluid renders as the missing texture");
        }
    }

    private static Path requiredFile(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "the build must set -D" + property);
        Path path = Path.of(value);
        assertTrue(Files.isRegularFile(path), value + " does not exist");
        return path;
    }

    private static List<String> methodNames(byte[] classBytes) {
        List<String> names = new ArrayList<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                names.add(name);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return names;
    }
}
