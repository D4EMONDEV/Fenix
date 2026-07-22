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

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that mod resources still reach the game.
 *
 * <p>Without this injection a mod's models, textures and translations sit in
 * its jar and are never read — the block registers and then renders as missing
 * texture with an untranslated name. That is a silent failure, so it is worth
 * a check that fails loudly instead.
 */
class ResourceMixinTest {

    private static final String TARGET = "net.minecraft.server.packs.repository.PackRepository";
    private static final String HANDLER = "fenix$addModPacks";

    @Test
    @DisplayName("the pack source injection still lands on PackRepository")
    void resourceMixinAppliesToRealMinecraft() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path resourceJar = requiredFile("fenix.test.resourceJar");

        Map<String, byte[]> transformed = new ConcurrentHashMap<>();

        try (FenixClassLoader loader = new FenixClassLoader(getClass().getClassLoader())) {
            loader.addPath(clientJar);
            loader.addPath(resourceJar);
            MixinSetup.bootstrap(loader, Side.CLIENT, List.of("fenix-api-resource.mixins.json"));
            loader.addTransformer((name, bytes) -> {
                transformed.put(name, bytes);
                return bytes;
            });

            loader.loadClass(TARGET);

            byte[] bytes = transformed.get(TARGET);
            assertNotNull(bytes, TARGET + " was never defined through the Fenix classloader");
            assertTrue(methodNames(bytes).stream().anyMatch(name -> name.contains(HANDLER)),
                    TARGET + " should carry " + HANDLER
                            + " — mod resources would silently never be loaded");
        }
    }

    @Test
    @DisplayName("a repository built with no folder still receives Fenix's source")
    void modPacksReachAFolderlessRepository() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path resourceJar = requiredFile("fenix.test.resourceJar");

        try (FenixClassLoader loader = new FenixClassLoader(getClass().getClassLoader())) {
            loader.addPath(clientJar);
            loader.addPath(resourceJar);
            MixinSetup.bootstrap(loader, Side.CLIENT, List.of("fenix-api-resource.mixins.json"));

            // ServerPacksSource reads the game's version while it initialises,
            // so the game has to have been brought up first.
            loader.loadClass("net.minecraft.SharedConstants")
                    .getMethod("tryDetectVersion").invoke(null);
            loader.loadClass("net.minecraft.server.Bootstrap")
                    .getMethod("bootStrap").invoke(null);

            // Vanilla's own name for the shape the create-world screen builds:
            // a ServerPacksSource and nothing else, with no folder anywhere.
            // Deciding a repository's kind by looking for a folder therefore
            // declined to touch it — so mod datapacks were absent from the load
            // that decides a new world's worldgen registries, an ore added to a
            // biome had no feature to point at, and nothing said so.
            Class<?> serverPacksSource =
                    loader.loadClass("net.minecraft.server.packs.repository.ServerPacksSource");
            Object repository = serverPacksSource
                    .getMethod("createVanillaTrustedRepository").invoke(null);

            assertTrue(carriesModPacks(loader, repository),
                    "a datapack repository with no folder should still receive Fenix's source"
                            + " — without it a mod contributes nothing to a new world, silently");
        }
    }

    /** {@return whether Fenix's source is among a repository's own} */
    private static boolean carriesModPacks(FenixClassLoader loader, Object repository)
            throws Exception {
        Field field = loader.loadClass("net.minecraft.server.packs.repository.PackRepository")
                .getDeclaredField("sources");
        field.setAccessible(true);

        Collection<?> sources = (Collection<?>) field.get(repository);
        return sources.stream().anyMatch(source ->
                source.getClass().getName().equals("fr.d4emon.fenix.resource.ModPackSource"));
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
