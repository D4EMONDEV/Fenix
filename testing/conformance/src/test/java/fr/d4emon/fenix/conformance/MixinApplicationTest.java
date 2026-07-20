package fr.d4emon.fenix.conformance;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import fr.d4emon.fenix.loader.mixin.MixinSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Proves the whole Mixin pipeline end to end: a mixin loaded from a jar
 * transforms a target class as it is defined by the {@link FenixClassLoader}.
 *
 * <p>No Minecraft required — the target is a synthetic class — but the code path
 * is exactly the one a real game class takes. This exists because the wiring it
 * covers (the service, the compatibility ceiling, the phase transition, the
 * load tracker) is subtle enough that a silent regression would otherwise only
 * show up as "my mixins stopped working" in front of a running game.
 *
 * <p>Mixin initialises once per JVM and never resets, so this test owns its
 * process — see {@code forkEvery = 1} in the build.
 */
class MixinApplicationTest {

    // The target must live outside the mixin config's package, or Mixin treats
    // it as a mixin class and refuses to load it (IllegalClassLoadError).
    private static final String TARGET = "fr.d4emon.fenix.probe.GreetingTarget";
    private static final String MIXIN = "fr.d4emon.fenix.mixin.probe.TargetMixin";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("a mixin in a jar rewrites a target class loaded through Fenix")
    void mixinAppliesToTarget() throws Exception {
        Path modJar = writeModJar();

        try (FenixClassLoader loader = new FenixClassLoader(getClass().getClassLoader())) {
            loader.addPath(modJar);
            MixinSetup.bootstrap(loader, Side.CLIENT, List.of("probe.mixins.json"));

            Class<?> target = loader.loadClass(TARGET);
            Object instance = target.getConstructor().newInstance();
            String greeting = (String) target.getMethod("greeting").invoke(instance);

            assertEquals("mixed", greeting,
                    "the mixin should have rewritten greeting() from 'vanilla' to 'mixed'");
        }
    }

    /**
     * Builds a jar holding the synthetic target, the compiled mixin's bytecode,
     * and the mixin config — the shape of a real mod jar.
     */
    private Path writeModJar() throws IOException {
        Path jar = tempDir.resolve("probe-mod.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            put(out, TARGET.replace('.', '/') + ".class", greetingTargetBytes());
            put(out, MIXIN.replace('.', '/') + ".class", compiledMixinBytes());
            put(out, "probe.mixins.json", resourceBytes("/probe.mixins.json"));
        }
        return jar;
    }

    private static void put(ZipOutputStream out, String name, byte[] bytes) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(bytes);
        out.closeEntry();
    }

    /** The mixin is compiled by the module; lift its bytecode off the test classpath. */
    private byte[] compiledMixinBytes() throws IOException {
        return resourceBytes("/" + MIXIN.replace('.', '/') + ".class");
    }

    private byte[] resourceBytes(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "missing test resource " + path);
            return in.readAllBytes();
        }
    }

    /** Synthesises {@code GreetingTarget} with a {@code String greeting()} returning "vanilla". */
    private static byte[] greetingTargetBytes() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                TARGET.replace('.', '/'), null, "java/lang/Object", null);

        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        MethodVisitor greeting = writer.visitMethod(
                Opcodes.ACC_PUBLIC, "greeting", "()Ljava/lang/String;", null, null);
        greeting.visitCode();
        greeting.visitLdcInsn("vanilla");
        greeting.visitInsn(Opcodes.ARETURN);
        greeting.visitMaxs(1, 1);
        greeting.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }
}
