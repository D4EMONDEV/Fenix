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
 * Checks that a mod's entities can still have attributes and a renderer.
 *
 * <p>Both tables are vanilla's and neither is open: the attribute one is an
 * {@code ImmutableMap} and the renderer one is private. If either injection
 * stops landing, a mod entity either cannot be constructed at all or is
 * invisible — and vanilla only warns about the second, once, in the log.
 */
class EntityMixinTest {

    private static final String ATTRIBUTES =
            "net.minecraft.world.entity.ai.attributes.DefaultAttributes";
    private static final String RENDERERS =
            "net.minecraft.client.renderer.entity.EntityRenderers";

    private static final String MOD_SUPPLIER = "fenix$modSupplier";
    private static final String MOD_HAS_SUPPLIER = "fenix$modHasSupplier";
    private static final String PROVIDERS = "fenix$providers";

    @Test
    @DisplayName("the entity injections still land on Minecraft's attribute and renderer tables")
    void entityMixinsApplyToRealMinecraft() throws Exception {
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

            loader.loadClass(ATTRIBUTES);
            loader.loadClass(RENDERERS);

            assertCarries(transformed, ATTRIBUTES, MOD_SUPPLIER,
                    "a mod's living entity would die in its own constructor");
            assertCarries(transformed, ATTRIBUTES, MOD_HAS_SUPPLIER,
                    "vanilla would report a mod's entity as having no attributes");
            assertCarries(transformed, RENDERERS, PROVIDERS,
                    "a mod's entities would all be invisible");
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
