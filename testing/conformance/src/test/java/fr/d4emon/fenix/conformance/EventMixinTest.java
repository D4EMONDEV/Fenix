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
    // ofEntries rather than of: Map.of stops at ten pairs, and there are more
    // events than that now.
    private static final Map<String, List<String>> EXPECTED_HANDLERS =
            new LinkedHashMap<>(Map.ofEntries(
            Map.entry("net.minecraft.server.MinecraftServer",
                    List.of("fenix$onTickStart", "fenix$onStopping")),
            Map.entry("net.minecraft.server.level.ServerPlayerGameMode",
                    List.of("fenix$onBreak", "fenix$onUse", "fenix$onUseItem")),
            Map.entry("net.minecraft.client.Minecraft",
                    List.of("fenix$onTickStart", "fenix$onScreen")),
            Map.entry("net.minecraft.client.multiplayer.MultiPlayerGameMode", List.of("fenix$onAttack")),
            Map.entry("net.minecraft.server.players.PlayerList", List.of("fenix$joined")),
            Map.entry("net.minecraft.world.entity.LivingEntity",
                    List.of("fenix$died", "fenix$onHurt")),
            // Right-clicking an entity, hooked on Player so the client
            // predicts it too rather than only the server deciding it.
            Map.entry("net.minecraft.world.entity.player.Player",
                    List.of("fenix$onInteract")),
            // Walking over an item, and arriving in another dimension.
            Map.entry("net.minecraft.world.entity.item.ItemEntity",
                    List.of("fenix$onPickup")),
            Map.entry("net.minecraft.server.level.ServerPlayer",
                    List.of("fenix$onChangedDimension")),
            Map.entry("net.minecraft.server.level.ServerLevel", List.of("fenix$spawning")),
            // A tooltip line and a client that has joined a world: both are
            // drawn or read on the client alone, and both are silent when the
            // injection stops landing — no tooltip line, no event, no message.
            Map.entry("net.minecraft.world.item.ItemStack", List.of("fenix$tooltip")),
            Map.entry("net.minecraft.client.multiplayer.ClientPacketListener", List.of("fenix$connected")),
            // Drawing over the HUD, and catching loot tables while they are
            // still a map rather than a frozen registry.
            Map.entry("net.minecraft.client.gui.Hud", List.of("fenix$hudRender")),
            Map.entry("net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener",
                    List.of("fenix$loadedLootTables"))));

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

            for (Map.Entry<String, List<String>> expected : EXPECTED_HANDLERS.entrySet()) {
                String target = expected.getKey();
                loader.loadClass(target);

                byte[] bytes = transformed.get(target);
                assertNotNull(bytes, target + " was never defined through the Fenix classloader");

                // Every handler the class should carry, not just one: three
                // events now land on classes that already had an injection, and
                // checking a single name would have shadowed them.
                for (String handler : expected.getValue()) {
                    assertTrue(methodNames(bytes).stream().anyMatch(name -> name.contains(handler)),
                            target + " should carry the mixin handler " + handler
                                    + " — the injection point has probably moved in this"
                                    + " Minecraft version");
                }
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
