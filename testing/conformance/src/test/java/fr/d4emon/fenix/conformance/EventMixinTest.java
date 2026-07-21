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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the event mixins still apply to real Minecraft.
 *
 * <p>This is the check that catches a Minecraft update moving or renaming a
 * method the events hang off. Without it, the failure mode is the worst kind:
 * an event that silently never fires, discovered by a player wondering why
 * their block protection stopped working.
 *
 * <p>Rather than trusting the config's strictness or reflecting on the loaded
 * class — which would need every Minecraft library on the classpath just to
 * resolve method signatures — this reads back the bytecode Mixin actually
 * produced and looks for the handler. Mixin renames private handlers with a
 * generated prefix, so the names are matched by {@code contains}, not equality.
 */
class EventMixinTest {

    /** Target class to a handler that must end up inside it. */
    private static final Map<String, String> EXPECTED_HANDLERS = new LinkedHashMap<>(Map.of(
            "net.minecraft.server.MinecraftServer", "fenix$onTickStart",
            "net.minecraft.server.level.ServerPlayerGameMode", "fenix$onBreak",
            "net.minecraft.client.Minecraft", "fenix$onTickStart",
            "net.minecraft.client.multiplayer.MultiPlayerGameMode", "fenix$onAttack"));

    @Test
    @DisplayName("every event mixin lands on its real Minecraft target")
    void eventMixinsApplyToRealMinecraft() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path eventJar = requiredFile("fenix.test.eventJar");

        Map<String, byte[]> transformed = new ConcurrentHashMap<>();

        try (FenixClassLoader loader = new FenixClassLoader(getClass().getClassLoader())) {
            loader.addPath(clientJar);
            loader.addPath(eventJar);
            MixinSetup.bootstrap(loader, Side.CLIENT, List.of("fenix-api-event.mixins.json"));

            // Registered after Mixin's, so it sees the finished bytecode.
            loader.addTransformer((name, bytes) -> {
                transformed.put(name, bytes);
                return bytes;
            });

            for (Map.Entry<String, String> expected : EXPECTED_HANDLERS.entrySet()) {
                String target = expected.getKey();
                String handler = expected.getValue();

                loader.loadClass(target);

                byte[] bytes = transformed.get(target);
                assertNotNull(bytes, target + " was never defined through the Fenix classloader");
                assertTrue(methodNames(bytes).stream().anyMatch(name -> name.contains(handler)),
                        target + " should carry the mixin handler " + handler
                                + " — the injection point has probably moved in this Minecraft version");
            }
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
