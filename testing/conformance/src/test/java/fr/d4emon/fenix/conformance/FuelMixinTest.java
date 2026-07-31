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
 * Checks that a mod's items can still be furnace fuel.
 *
 * <p>The other five block interactions are proven by behaviour, in
 * {@code RegistryProbe}: it asks vanilla's own lookups what a modded block
 * burns like, strips into, waxes into and weathers into, and compares. Fuel
 * cannot be asked the same way — every lookup on it takes an {@code ItemStack},
 * and a stack cannot exist until data components are bound, which happens while
 * datapacks load and long after a probe stops. Vanilla's own {@code Items.STONE}
 * is equally unstackable there, so the limit is the process, not the mod.
 *
 * <p>What can be proven is that the injection lands, which is the part that
 * breaks under a game update. Without it a modded coal is refused by the fuel
 * slot with no message at all.
 */
class FuelMixinTest {

    private static final String TARGET = "net.minecraft.world.level.block.entity.FuelValues";
    private static final String BURN_DURATION = "fenix$modBurnDuration";
    private static final String IS_FUEL = "fenix$modIsFuel";

    @Test
    @DisplayName("the fuel injections still land on Minecraft's burn-time table")
    void fuelMixinAppliesToRealMinecraft() throws Exception {
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

            loader.loadClass(TARGET);

            byte[] bytes = transformed.get(TARGET);
            assertNotNull(bytes, TARGET + " was never defined through the Fenix classloader");
            List<String> methods = methodNames(bytes);
            // Mixin renames private handlers as it merges them, so the name it
            // was written under is a substring of what ends up in the class.
            assertTrue(methods.stream().anyMatch(name -> name.contains(BURN_DURATION)),
                    TARGET + " should carry " + BURN_DURATION
                            + " — a mod's fuel would burn for zero ticks");
            assertTrue(methods.stream().anyMatch(name -> name.contains(IS_FUEL)),
                    TARGET + " should carry " + IS_FUEL
                            + " — a furnace would refuse a mod's fuel outright");
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
