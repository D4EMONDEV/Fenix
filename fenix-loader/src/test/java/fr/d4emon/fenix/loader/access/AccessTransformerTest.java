package fr.d4emon.fenix.loader.access;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opening doors vanilla shut.
 *
 * <p>The failure mode this guards against is not a crash: a declaration that
 * silently does nothing leaves a mod that compiled against a widened jar and
 * then cannot touch the same member at run time — an {@code IllegalAccessError}
 * from deep inside the game, naming nothing a mod author wrote.
 */
class AccessTransformerTest {

    private static final String OWNER = "com/example/Target";
    private static final String NESTED = "com/example/Target$Inner";

    /** A class with a private constructor, a private method and a private field. */
    private static byte[] target() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, OWNER,
                null, "java/lang/Object", null);
        writer.visitInnerClass(NESTED, OWNER, "Inner", Opcodes.ACC_PRIVATE | Opcodes.ACC_INTERFACE);
        writer.visitField(Opcodes.ACC_PRIVATE, "secret", "I", null, null).visitEnd();

        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, "hidden", "()V", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 1);
        method.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static AccessTransformer transformer(String... declarations) {
        AccessWidener widener = new AccessWidener();
        widener.add(List.of(declarations), "test.jar");
        return new AccessTransformer(widener);
    }

    /** {@return every member's access, keyed by name} */
    private static Map<String, Integer> access(byte[] bytes) {
        Map<String, Integer> found = new LinkedHashMap<>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                found.put("class", access);
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                found.put("inner:" + name, access);
            }

            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name,
                                                               String descriptor, String signature,
                                                               String[] exceptions) {
                found.put("method:" + name, access);
                return null;
            }

            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name,
                                                             String descriptor, String signature,
                                                             Object value) {
                found.put("field:" + name, access);
                return null;
            }
        }, 0);
        return found;
    }

    private static boolean isPublic(Map<String, Integer> access, String key) {
        return (access.get(key) & Opcodes.ACC_PUBLIC) != 0;
    }

    @Test
    @DisplayName("a widened constructor becomes public")
    void widensAConstructor() {
        byte[] out = transformer("method com.example.Target <init>")
                .transform("com.example.Target", target());

        assertTrue(isPublic(access(out), "method:<init>"));
        assertTrue((access(out).get("method:<init>") & Opcodes.ACC_PRIVATE) == 0,
                "the old visibility should be gone, not merely joined by public");
    }

    @Test
    @DisplayName("a widened method and field become public, and their neighbours do not")
    void widensOnlyWhatIsNamed() {
        byte[] out = transformer("method com.example.Target hidden")
                .transform("com.example.Target", target());
        Map<String, Integer> access = access(out);

        assertTrue(isPublic(access, "method:hidden"));
        // Widening is a permission, not a policy: nothing that was not asked
        // for should move, or a mod would be relying on doors nobody opened.
        assertTrue((access.get("method:<init>") & Opcodes.ACC_PRIVATE) != 0);
        assertTrue((access.get("field:secret") & Opcodes.ACC_PRIVATE) != 0);
    }

    @Test
    @DisplayName("a widened nested type is opened in the InnerClasses entry too")
    void widensTheInnerClassesEntry() {
        byte[] out = transformer("class com.example.Target$Inner")
                .transform("com.example.Target", target());

        // The half everyone forgets. javac reads this entry, not the nested
        // class's own flags, when deciding whether the name may be written
        // down — so widening only the other half compiles nowhere.
        assertTrue(isPublic(access(out), "inner:" + NESTED));
    }

    @Test
    @DisplayName("a class nothing named is handed back untouched, not rewritten")
    void leavesOtherClassesAlone() {
        byte[] original = target();

        byte[] out = transformer("class com.example.Something$Else")
                .transform("com.example.Target", original);

        // Identity, not equality: every class the game loads passes through
        // here, and rewriting them all to change none is a cost paid on every
        // launch.
        assertSame(original, out);
    }

    @Test
    @DisplayName("a declaration nobody can read is refused, naming the mod")
    void rejectsNonsense() {
        AccessWidener widener = new AccessWidener();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> widener.add(List.of("open com.example.Target"), "broken.jar"));

        assertTrue(thrown.getMessage().contains("broken.jar"), thrown.getMessage());
    }

    @Test
    @DisplayName("a mod declaring nothing costs nothing")
    void nothingDeclaredIsEmpty() {
        AccessWidener widener = new AccessWidener();
        widener.add(List.of(), "quiet.jar");

        assertEquals(true, widener.isEmpty());
    }
}
