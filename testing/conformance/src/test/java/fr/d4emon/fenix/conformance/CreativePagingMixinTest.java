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
 * Checks that creative pages still work.
 *
 * <p>Vanilla's tab strip holds exactly fourteen tabs and vanilla fills all
 * fourteen, so a mod tab only has anywhere to go because of these two
 * injections. If either stops landing, a mod's tab does not appear at all —
 * and nothing crashes to say so.
 */
class CreativePagingMixinTest {

    private static final String TABS = "net.minecraft.world.item.CreativeModeTabs";
    private static final String SCREEN =
            "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen";

    private static final String PAGE_FILTER = "fenix$onlyCurrentPage";
    private static final String PER_PAGE_VALIDATION = "fenix$validatePerPage";
    private static final String ADD_BUTTONS = "fenix$addPageButtons";
    private static final String PAGE_KEYS = "fenix$pageKeys";

    @Test
    @DisplayName("the paging injections still land on Minecraft's tabs and creative screen")
    void pagingMixinsApplyToRealMinecraft() throws Exception {
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

            loader.loadClass(TABS);
            loader.loadClass(SCREEN);

            assertCarries(transformed, TABS, PAGE_FILTER,
                    "every tab would be crammed into vanilla's fourteen slots");
            assertCarries(transformed, TABS, PER_PAGE_VALIDATION,
                    "bootstrap would refuse to start the moment a mod registers a tab");
            assertCarries(transformed, SCREEN, ADD_BUTTONS,
                    "there would be no arrows to change page with");
            assertCarries(transformed, SCREEN, PAGE_KEYS,
                    "Page Up and Page Down would stop turning pages");
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
