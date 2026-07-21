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
 * Checks that mods can still add commands.
 *
 * <p>One injection carries all of them, at the tail of the constructor that
 * builds the command tree. If it stops landing, every mod command is simply
 * absent — the server starts, plays fine, and answers "Unknown command" to
 * something the mod is certain it registered.
 */
class CommandMixinTest {

    private static final String TARGET = "net.minecraft.commands.Commands";
    private static final String HANDLER = "fenix$openTheTree";

    @Test
    @DisplayName("the command injection still lands on Minecraft's command tree")
    void commandMixinAppliesToRealMinecraft() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path commandJar = requiredFile("fenix.test.commandJar");
        Path eventJar = requiredFile("fenix.test.eventJar");

        Map<String, byte[]> transformed = new ConcurrentHashMap<>();

        try (FenixClassLoader loader = new FenixClassLoader(getClass().getClassLoader())) {
            loader.addPath(clientJar);
            loader.addPath(commandJar);
            // Commands are announced through the event bus, so it has to be
            // reachable for the mixin's own class to load.
            loader.addPath(eventJar);
            MixinSetup.bootstrap(loader, Side.CLIENT, List.of("fenix-api-command.mixins.json"));
            loader.addTransformer((name, bytes) -> {
                transformed.put(name, bytes);
                return bytes;
            });

            loader.loadClass(TARGET);

            byte[] bytes = transformed.get(TARGET);
            assertNotNull(bytes, TARGET + " was never defined through the Fenix classloader");
            assertTrue(methodNames(bytes).stream().anyMatch(name -> name.contains(HANDLER)),
                    TARGET + " should carry " + HANDLER
                            + " — without it every mod command is silently absent");
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
