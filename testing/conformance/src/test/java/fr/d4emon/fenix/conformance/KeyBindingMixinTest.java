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
 * Checks that a mod's key bindings still reach the game's own list.
 *
 * <p>Vanilla builds that list once, in a field initialiser naming its own
 * mappings one by one, and never reads it again. A mapping missing from it
 * never appears in the controls screen and is never saved to
 * {@code options.txt} — so the key works until the player restarts and then
 * silently returns to its default. Nothing crashes, and nothing is logged.
 */
class KeyBindingMixinTest {

    private static final String OPTIONS = "net.minecraft.client.Options";
    private static final String HANDLER = "fenix$addModKeyBindings";
    private static final String FIELD = "keyMappings";

    @Test
    @DisplayName("the key binding injection still lands on Minecraft's options")
    void keyBindingMixinAppliesToRealMinecraft() throws Exception {
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

            loader.loadClass(OPTIONS);

            byte[] bytes = transformed.get(OPTIONS);
            assertNotNull(bytes, OPTIONS + " was never defined through the Fenix classloader");

            // Mixin renames private handlers as it merges them, so the name is
            // a substring of what ends up in the class rather than all of it.
            assertTrue(methodNames(bytes).stream().anyMatch(name -> name.contains(HANDLER)),
                    "Options should carry " + HANDLER + " — without it a mod's keys are "
                            + "absent from the controls screen and never saved");

            // The list is final in vanilla, and extending it means replacing
            // it. If @Mutable ever stops stripping that flag the injection
            // still lands and the assignment inside it does nothing.
            assertFalse(isFinal(bytes, FIELD),
                    "keyMappings should no longer be final, or the mod's keys are dropped "
                            + "on the floor by an assignment that cannot happen");
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
