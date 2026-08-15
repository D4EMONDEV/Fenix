package fr.d4emon.fenix.conformance;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import fr.d4emon.fenix.loader.mixin.MixinSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a mod can still add a feature to a biome it does not own.
 *
 * <p>The alternative to these two injections is overriding whole biome files in
 * a datapack, where two mods each adding an ore to the plains erase one
 * another. If either stops landing, a mod's ore stops generating — in a world
 * that loads fine, with nothing logged, and looks exactly like bad luck.
 */
class BiomeMixinTest {

    private static final String LOADER = "net.minecraft.resources.RegistryDataLoader";
    private static final String SETTINGS = "net.minecraft.world.level.biome.BiomeGenerationSettings";
    private static final String SPAWNS = "net.minecraft.world.level.biome.MobSpawnSettings";

    private static final String APPLY = "fenix$applyBiomeModifications";
    private static final String ADD = "fenix$addFeature";
    private static final String FIELD = "features";
    private static final String ADD_SPAWN = "fenix$addSpawn";
    private static final String REMOVE_SPAWN = "fenix$removeSpawn";
    private static final String SPAWN_FIELD = "spawners";

    @Test
    @DisplayName("the biome injections still land on Minecraft's registry loader and biome settings")
    void biomeMixinsApplyToRealMinecraft() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path registryJar = requiredFile("fenix.test.registryJar");

        Map<String, byte[]> transformed = new ConcurrentHashMap<>();

        try (FenixClassLoader loader = new FenixClassLoader(getClass().getClassLoader())) {
            loader.addPath(clientJar);
            loader.addPath(registryJar);
            MixinSetup.bootstrap(loader, Side.SERVER, List.of("fenix-api-registry.mixins.json"));
            loader.addTransformer((name, bytes) -> {
                transformed.put(name, bytes);
                return bytes;
            });

            loader.loadClass(LOADER);
            loader.loadClass(SETTINGS);
            loader.loadClass(SPAWNS);

            assertCarries(transformed, LOADER, APPLY,
                    "no modification would ever be applied, to any biome");
            assertCarries(transformed, SETTINGS, ADD,
                    "there would be nothing to apply them with");

            // And the interface that lets it be called. A mixin class is a
            // template: Mixin merges its members into the target and then
            // refuses to load the template itself, so a caller has to cast to
            // something ordinary. Without the interface on the target that cast
            // throws IllegalClassLoadError the first time a feature is actually
            // applied — which is to say, in a working installation and nowhere
            // else. That is how it reached a player before it reached a test.
            assertTrue(interfaces(transformed.get(SETTINGS))
                            .contains("fr/d4emon/fenix/registry/worldgen/BiomeFeatureAccess"),
                    "BiomeGenerationSettings should implement BiomeFeatureAccess — without it "
                            + "applying a modification throws IllegalClassLoadError");

            // The list is final in vanilla, and adding to it means replacing it.
            // If @Mutable ever stops stripping that flag the method still merges
            // and its assignment does nothing.
            assertFalse(isFinal(transformed.get(SETTINGS), FIELD),
                    "the feature list should no longer be final, or the feature is added to "
                            + "a list that is then thrown away");

            // The same three things again, for spawns. A mob that is registered
            // but never added to a biome can only be summoned by hand, and
            // nothing in the log says why — so every step of the path that puts
            // one in the world is worth a check of its own.
            assertCarries(transformed, SPAWNS, ADD_SPAWN,
                    "no mob would ever be added to a biome's spawn table");
            assertCarries(transformed, SPAWNS, REMOVE_SPAWN,
                    "a mod could add a spawn but never take one away");

            assertTrue(interfaces(transformed.get(SPAWNS))
                            .contains("fr/d4emon/fenix/registry/worldgen/BiomeSpawnAccess"),
                    "MobSpawnSettings should implement BiomeSpawnAccess — without it "
                            + "adding a spawn throws IllegalClassLoadError");

            assertFalse(isFinal(transformed.get(SPAWNS), SPAWN_FIELD),
                    "the spawn table should no longer be final, or the mob is added to "
                            + "a map that is then thrown away");
        }
    }

    private static void assertCarries(Map<String, byte[]> transformed, String target,
                                      String handler, String consequence) {
        byte[] bytes = transformed.get(target);
        assertNotNull(bytes, target + " was never defined through the Fenix classloader");
        // Mixin renames private handlers as it merges them, so the name is a
        // substring of what ends up in the class rather than all of it.
        assertTrue(methodNames(bytes).stream().anyMatch(name -> name.contains(handler)),
                target + " should carry " + handler + " — without it " + consequence);
    }

    /** {@return the interfaces a class declares} */
    private static List<String> interfaces(byte[] classBytes) {
        List<String> found = new ArrayList<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] declared) {
                if (declared != null) {
                    found.addAll(List.of(declared));
                }
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found;
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

    private static boolean isFinal(byte[] classBytes, String field) {
        boolean[] result = {false};
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                if (name.equals(field)) {
                    result[0] = (access & Opcodes.ACC_FINAL) != 0;
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return result[0];
    }
}
