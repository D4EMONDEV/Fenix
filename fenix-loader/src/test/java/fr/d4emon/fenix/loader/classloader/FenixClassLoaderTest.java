package fr.d4emon.fenix.loader.classloader;

import fr.d4emon.fenix.api.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FenixClassLoaderTest {

    private static final String SHARED_PROBE = "fr.d4emon.fenix.testprobe.SharedProbe";

    @TempDir
    Path tempDir;

    private FenixClassLoader loader;

    @BeforeEach
    void createLoader() {
        loader = new FenixClassLoader(getClass().getClassLoader());
    }

    @AfterEach
    void closeLoader() throws IOException {
        // Also releases the jar file locks, without which Windows cannot
        // delete the temporary directory.
        loader.close();
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    /**
     * Generates a minimal class whose static {@code source()} returns the given
     * value — enough to tell two copies of a class apart at runtime.
     */
    private static byte[] classReturning(String binaryName, String value) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                binaryName.replace('.', '/'), null, "java/lang/Object", null);

        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        MethodVisitor source = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "source", "()Ljava/lang/String;", null, null);
        source.visitCode();
        source.visitLdcInsn(value);
        source.visitInsn(Opcodes.ARETURN);
        source.visitMaxs(1, 0);
        source.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private Path writeJar(String fileName, Map<String, byte[]> entries) throws IOException {
        Path jar = tempDir.resolve(fileName);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return jar;
    }

    /** Adds a jar holding one generated class and returns the jar's path. */
    private Path addJarWithClass(String binaryName, String value) throws IOException {
        Path jar = writeJar(binaryName.substring(binaryName.lastIndexOf('.') + 1) + ".jar",
                Map.of(binaryName.replace('.', '/') + ".class", classReturning(binaryName, value)));
        loader.addPath(jar);
        return jar;
    }

    private static String source(Class<?> type) throws Exception {
        return (String) type.getMethod("source").invoke(null);
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("child-first order")
    class ChildFirst {

        @Test
        @DisplayName("a class present in a jar and on the classpath comes from the jar")
        void childShadowsTheClasspath() throws Exception {
            addJarWithClass(SHARED_PROBE, "child");

            Class<?> viaFenix = loader.loadClass(SHARED_PROBE);

            assertEquals("child", source(viaFenix));
            assertSame(loader, viaFenix.getClassLoader());
            // The classpath copy is untouched and distinct.
            assertEquals("parent", fr.d4emon.fenix.testprobe.SharedProbe.source());
            assertNotSame(fr.d4emon.fenix.testprobe.SharedProbe.class, viaFenix);
        }

        @Test
        @DisplayName("a class only on the classpath still resolves, from the parent")
        void fallsBackToTheParent() throws Exception {
            Class<?> viaFenix = loader.loadClass(SHARED_PROBE);

            assertSame(fr.d4emon.fenix.testprobe.SharedProbe.class, viaFenix);
        }

        @Test
        void aClassNobodyHasIsNotFound() {
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass("com.example.Ghost"));
        }

        @Test
        @DisplayName("loading twice yields the same class object")
        void definesEachClassOnce() throws Exception {
            addJarWithClass("com.example.Once", "child");

            assertSame(loader.loadClass("com.example.Once"), loader.loadClass("com.example.Once"));
        }

        @Test
        @DisplayName("the defined class knows which jar it came from")
        void recordsTheCodeSource() throws Exception {
            Path jar = addJarWithClass("com.example.Located", "child");

            URL location = loader.loadClass("com.example.Located")
                    .getProtectionDomain().getCodeSource().getLocation();

            assertTrue(location.toString().endsWith(jar.getFileName().toString()),
                    "expected " + location + " to end with " + jar.getFileName());
        }
    }

    @Nested
    @DisplayName("parent-only prefixes")
    class ParentOnly {

        @Test
        @DisplayName("an API class in a mod jar is ignored — the parent's copy is the only one")
        void apiClassesCannotBeShadowed() throws Exception {
            addJarWithClass("fr.d4emon.fenix.api.Version", "impostor");

            Class<?> viaFenix = loader.loadClass("fr.d4emon.fenix.api.Version");

            assertSame(Version.class, viaFenix);
        }

        @Test
        @DisplayName("loader classes always come from the parent")
        void loaderClassesCannotBeShadowed() throws Exception {
            addJarWithClass("fr.d4emon.fenix.loader.classloader.FenixClassLoader", "impostor");

            assertSame(FenixClassLoader.class,
                    loader.loadClass("fr.d4emon.fenix.loader.classloader.FenixClassLoader"));
        }

        @Test
        @DisplayName("a parent-only class the parent lacks fails, even if a jar offers it")
        void neverFallsBackToTheChild() throws IOException {
            addJarWithClass("fr.d4emon.fenix.api.OnlyInThisJar", "impostor");

            assertThrows(ClassNotFoundException.class,
                    () -> loader.loadClass("fr.d4emon.fenix.api.OnlyInThisJar"));
        }
    }

    @Nested
    @DisplayName("transformation")
    class Transformation {

        @Test
        void rewritesAChildClassOnDefinition() throws Exception {
            addJarWithClass("com.example.Target", "original");
            loader.addTransformer((name, bytes) ->
                    name.equals("com.example.Target") ? classReturning(name, "transformed") : bytes);

            assertEquals("transformed", source(loader.loadClass("com.example.Target")));
        }

        @Test
        @DisplayName("transformers chain in registration order")
        void chainsInOrder() throws Exception {
            addJarWithClass("com.example.Chained", "original");
            List<String> calls = new ArrayList<>();
            loader.addTransformer((name, bytes) -> {
                calls.add("first");
                return classReturning(name, "first");
            });
            loader.addTransformer((name, bytes) -> {
                calls.add("second");
                return classReturning(name, "second");
            });

            assertEquals("second", source(loader.loadClass("com.example.Chained")));
            assertEquals(List.of("first", "second"), calls);
        }

        @Test
        @DisplayName("parent classes are never offered to transformers")
        void neverSeesParentClasses() throws Exception {
            List<String> seen = new ArrayList<>();
            loader.addTransformer((name, bytes) -> {
                seen.add(name);
                return bytes;
            });

            loader.loadClass("fr.d4emon.fenix.api.Version");   // parent-only prefix
            loader.loadClass(SHARED_PROBE);                    // parent fallback

            assertEquals(List.of(), seen);
        }

        @Test
        @DisplayName("a transformer failure stops the launch instead of running the original")
        void aThrowingTransformerIsFatal() throws Exception {
            addJarWithClass("com.example.Doomed", "original");
            RuntimeException boom = new IllegalStateException("boom");
            loader.addTransformer((name, bytes) -> {
                throw boom;
            });

            ClassTransformationException failure = assertThrows(ClassTransformationException.class,
                    () -> loader.loadClass("com.example.Doomed"));

            assertTrue(failure.getMessage().contains("com.example.Doomed"), failure.getMessage());
            assertSame(boom, failure.getCause());
        }

        @Test
        void aNullReturnIsReportedAsABug() throws Exception {
            addJarWithClass("com.example.Nulled", "original");
            loader.addTransformer((name, bytes) -> null);

            ClassTransformationException failure = assertThrows(ClassTransformationException.class,
                    () -> loader.loadClass("com.example.Nulled"));

            assertTrue(failure.getMessage().contains("com.example.Nulled"), failure.getMessage());
        }
    }

    @Nested
    @DisplayName("resources")
    class Resources {

        @Test
        @DisplayName("a resource in a jar shadows the classpath copy")
        void childResourceWins() throws Exception {
            loader.addPath(writeJar("resources.jar",
                    Map.of("marker.txt", "child".getBytes(StandardCharsets.UTF_8))));

            try (InputStream in = loader.getResourceAsStream("marker.txt")) {
                assertEquals("child", new String(in.readAllBytes(), StandardCharsets.UTF_8).strip());
            }
        }

        @Test
        void fallsBackToTheClasspathForEverythingElse() throws Exception {
            try (InputStream in = loader.getResourceAsStream("parent-only.txt")) {
                assertEquals("only on the classpath",
                        new String(in.readAllBytes(), StandardCharsets.UTF_8).strip());
            }
        }

        @Test
        @DisplayName("getResources lists every copy, child first")
        void listsAllCopiesChildFirst() throws Exception {
            loader.addPath(writeJar("resources.jar",
                    Map.of("marker.txt", "child".getBytes(StandardCharsets.UTF_8))));

            List<URL> all = Collections.list(loader.getResources("marker.txt"));

            assertTrue(all.size() >= 2, "expected the jar copy and the classpath copy, got " + all);
            assertTrue(all.getFirst().toString().contains("resources.jar"),
                    "expected the child copy first, got " + all);
        }

        @Test
        void aResourceNobodyHasIsNull() {
            assertNull(loader.getResource("no/such/resource.txt"));
        }
    }

    @Nested
    @DisplayName("arguments")
    class Arguments {

        @Test
        void rejectsNulls() {
            assertThrows(NullPointerException.class, () -> new FenixClassLoader(null));
            assertThrows(NullPointerException.class, () -> loader.addPath(null));
            assertThrows(NullPointerException.class, () -> loader.addTransformer(null));
        }
    }
}
