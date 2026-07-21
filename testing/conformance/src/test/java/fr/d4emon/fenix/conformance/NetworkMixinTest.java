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
 * Checks that mod payloads can still reach vanilla's wire.
 *
 * <p>Three injections carry every packet a mod sends. If the codec one stops
 * landing, everything a mod sends is decoded as a discarded payload and
 * disappears without a word — vanilla's own behaviour for an id it does not
 * know. If either delivery one stops landing, packets arrive and no handler
 * ever runs. None of it crashes.
 */
class NetworkMixinTest {

    private static final String PAYLOAD = "net.minecraft.network.protocol.common.custom.CustomPacketPayload";
    private static final String SERVER_LISTENER = "net.minecraft.server.network.ServerGamePacketListenerImpl";
    private static final String CLIENT_LISTENER =
            "net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl";

    private static final String ADD_ENVELOPES = "fenix$addEnvelopes";
    private static final String DELIVER = "fenix$deliver";

    @Test
    @DisplayName("the payload injections still land on Minecraft's codec and both listeners")
    void networkMixinsApplyToRealMinecraft() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path networkJar = requiredFile("fenix.test.networkJar");

        Map<String, byte[]> transformed = new ConcurrentHashMap<>();

        try (FenixClassLoader loader = new FenixClassLoader(getClass().getClassLoader())) {
            loader.addPath(clientJar);
            loader.addPath(networkJar);
            MixinSetup.bootstrap(loader, Side.CLIENT, List.of("fenix-api-network.mixins.json"));
            loader.addTransformer((name, bytes) -> {
                transformed.put(name, bytes);
                return bytes;
            });

            loader.loadClass(PAYLOAD);
            loader.loadClass(SERVER_LISTENER);
            loader.loadClass(CLIENT_LISTENER);

            assertCarries(transformed, PAYLOAD, ADD_ENVELOPES,
                    "everything a mod sends would be discarded silently, as an unknown id");
            assertCarries(transformed, SERVER_LISTENER, DELIVER,
                    "the server would receive payloads and hand them to nobody");
            assertCarries(transformed, CLIENT_LISTENER, DELIVER,
                    "a client would receive payloads and hand them to nobody");
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
