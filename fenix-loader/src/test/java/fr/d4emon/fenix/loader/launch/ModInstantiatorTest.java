package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import fr.d4emon.fenix.loader.discovery.ModCandidate;
import fr.d4emon.fenix.loader.metadata.ModMetadata;
import fr.d4emon.fenix.loader.metadata.ModSide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModInstantiatorTest {

    @TempDir
    Path tempDir;

    private FenixClassLoader loader;

    @BeforeEach
    void createLoader() {
        loader = new FenixClassLoader(getClass().getClassLoader());
    }

    @AfterEach
    void closeLoader() throws IOException {
        loader.close();
    }

    /** Generates a concrete, empty FenixMod implementation — the default methods do the rest. */
    private static byte[] emptyModClass(String binaryName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, binaryName.replace('.', '/'),
                null, "java/lang/Object", new String[] {"fr/d4emon/fenix/api/FenixMod"});
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    /** Generates a class that does NOT implement FenixMod. */
    private static byte[] plainClass(String binaryName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, binaryName.replace('.', '/'),
                null, "java/lang/Object", null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private ModCandidate candidate(String id, String indexedId, String className, byte[] classBytes)
            throws IOException {
        Path jar = tempDir.resolve(id + ".jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry(ModIndexReader.FILE_NAME));
            out.write("""
                    {"schema": 1, "mods": {"%s": "%s"}}
                    """.formatted(indexedId, className).getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            if (classBytes != null) {
                out.putNextEntry(new ZipEntry(className.replace('.', '/') + ".class"));
                out.write(classBytes);
                out.closeEntry();
            }
        }
        loader.addPath(jar);
        ModMetadata metadata = new ModMetadata(id, Version.parse("1.0.0"),
                null, null, null, null, null, ModSide.BOTH, null, null);
        return new ModCandidate(metadata, jar);
    }

    @Test
    @DisplayName("a well-formed jar yields an instantiated mod, loaded by the child")
    void instantiatesFromTheJar() throws IOException {
        ModCandidate candidate = candidate("real-mod", "real-mod",
                "com.example.RealMod", emptyModClass("com.example.RealMod"));

        List<LoadedMod> mods = ModInstantiator.instantiate(loader, List.of(candidate), Side.CLIENT);

        assertEquals(1, mods.size());
        assertEquals(1, mods.getFirst().entries().size());
        assertSame(loader, mods.getFirst().entries().getFirst().getClass().getClassLoader());
    }

    @Test
    @DisplayName("a server never loads a mod's client entry class")
    void aServerSkipsTheClientHalf() throws IOException {
        ModCandidate candidate = splitCandidate();

        // The client class is deliberately absent from the jar: a server that
        // tried to load it would fail, which is exactly the point. Nothing
        // there can resolve against a server jar that ships no client classes,
        // so the server must not even be told the class exists.
        List<LoadedMod> onServer =
                ModInstantiator.instantiate(loader, List.of(candidate), Side.SERVER);

        assertEquals(1, onServer.getFirst().entries().size(),
                "a server should load the common entry class and nothing else");
    }

    @Test
    @DisplayName("a client loads both halves, common first")
    void aClientLoadsBothHalves() throws IOException {
        ModCandidate candidate = splitCandidate();
        loader.addPath(writeClass("com.example.SplitClient"));

        List<LoadedMod> onClient =
                ModInstantiator.instantiate(loader, List.of(candidate), Side.CLIENT);

        List<String> loaded = onClient.getFirst().entries().stream()
                .map(entry -> entry.getClass().getName())
                .toList();
        // Order matters: the client half is written against content the common
        // half registers, so it has to run second.
        assertEquals(List.of("com.example.Split", "com.example.SplitClient"), loaded);
    }

    /** A mod jar with both indexes, but only the common entry class present. */
    private ModCandidate splitCandidate() throws IOException {
        Path jar = tempDir.resolve("split.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry(ModIndexReader.FILE_NAME));
            out.write("""
                    {"schema": 1, "mods": {"split": "com.example.Split"}}
                    """.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry(ModIndexReader.CLIENT_FILE_NAME));
            out.write("""
                    {"schema": 1, "mods": {"split": "com.example.SplitClient"}}
                    """.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("com/example/Split.class"));
            out.write(emptyModClass("com.example.Split"));
            out.closeEntry();
        }
        loader.addPath(jar);
        ModMetadata metadata = new ModMetadata("split", Version.parse("1.0.0"),
                null, null, null, null, null, ModSide.BOTH, null, null);
        return new ModCandidate(metadata, jar);
    }

    /** {@return a one-class jar, for the half a server never sees} */
    private Path writeClass(String className) throws IOException {
        Path jar = tempDir.resolve(className + ".jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry(className.replace('.', '/') + ".class"));
            out.write(emptyModClass(className));
            out.closeEntry();
        }
        return jar;
    }

    @Test
    @DisplayName("a jar with no index is a data-only mod with zero entries")
    void toleratesADataOnlyMod() throws IOException {
        Path jar = tempDir.resolve("data-only.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry("assets/thing.png"));
            out.closeEntry();
        }
        ModMetadata metadata = new ModMetadata("data-only", Version.parse("1.0.0"),
                null, null, null, null, null, ModSide.BOTH, null, null);

        List<LoadedMod> mods = ModInstantiator.instantiate(loader, List.of(new ModCandidate(metadata, jar)), Side.CLIENT);

        assertEquals(List.of(), mods.getFirst().entries());
    }

    @Test
    void rejectsAnIndexForADifferentMod() throws IOException {
        ModCandidate candidate = candidate("declared-id", "other-id",
                "com.example.Whatever", emptyModClass("com.example.Whatever"));

        LaunchException failure = assertThrows(LaunchException.class,
                () -> ModInstantiator.instantiate(loader, List.of(candidate), Side.CLIENT));

        assertTrue(failure.getMessage().contains("declared-id"), failure.getMessage());
        assertTrue(failure.getMessage().contains("other-id"), failure.getMessage());
    }

    @Test
    void reportsAMissingEntryClass() throws IOException {
        ModCandidate candidate = candidate("ghost-mod", "ghost-mod", "com.example.Vanished", null);

        LaunchException failure = assertThrows(LaunchException.class,
                () -> ModInstantiator.instantiate(loader, List.of(candidate), Side.CLIENT));

        assertTrue(failure.getMessage().contains("com.example.Vanished"), failure.getMessage());
        assertTrue(failure.getMessage().contains("ghost-mod"), failure.getMessage());
    }

    @Test
    void reportsAClassThatIsNotAMod() throws IOException {
        ModCandidate candidate = candidate("liar-mod", "liar-mod",
                "com.example.NotAMod", plainClass("com.example.NotAMod"));

        LaunchException failure = assertThrows(LaunchException.class,
                () -> ModInstantiator.instantiate(loader, List.of(candidate), Side.CLIENT));

        assertTrue(failure.getMessage().contains("does not implement"), failure.getMessage());
    }
}
