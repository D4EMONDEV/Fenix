package fr.d4emon.fenix.loader.access;

import fr.d4emon.fenix.loader.classloader.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

/**
 * Raises the declared access of what a mod asked to reach.
 *
 * <p>Runs before Mixin, because a mixin that targets a widened member has to
 * see it already widened — and because Mixin's own transformer is registered
 * after this one, which is what decides the order.
 *
 * <p>Making a nested type nameable takes two edits, not one. The type's own
 * access flags are the obvious half; the other is the {@code InnerClasses}
 * entry, which both the outer and the nested class carry and which is what
 * {@code javac} actually reads when deciding whether you may write the name
 * down. Widening only the first leaves a class that is public at runtime and
 * still refuses to compile.
 */
public final class AccessTransformer implements ClassTransformer {

    private static final int VISIBILITY = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED
            | Opcodes.ACC_PRIVATE;

    private final AccessWidener widener;

    /**
     * Widens what a set of declarations names.
     *
     * @param widener what to widen
     */
    public AccessTransformer(AccessWidener widener) {
        this.widener = Objects.requireNonNull(widener, "widener");
    }

    @Override
    public byte[] transform(String className, byte[] bytes) {
        String internalName = className.replace('.', '/');
        if (!widener.touches(internalName)) {
            // The common case by a wide margin: every class the game loads goes
            // past here, and rewriting them all to change none would be a cost
            // paid on every launch.
            return bytes;
        }
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new Widening(writer, internalName), 0);
        return writer.toByteArray();
    }

    /** {@return the access with its visibility replaced by public} */
    private static int publicised(int access) {
        return (access & ~VISIBILITY) | Opcodes.ACC_PUBLIC;
    }

    private final class Widening extends ClassVisitor {

        private final String internalName;

        Widening(ClassVisitor next, String internalName) {
            super(Opcodes.ASM9, next);
            this.internalName = internalName;
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, widener.widensClass(name) ? publicised(access) : access,
                    name, signature, superName, interfaces);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            // The half everyone forgets. javac reads this, not the class's own
            // flags, when deciding whether a nested type may be named.
            super.visitInnerClass(name, outerName, innerName,
                    widener.widensClass(name) ? publicised(access) : access);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            return super.visitMethod(
                    widener.widensMethod(internalName, name) ? publicised(access) : access,
                    name, descriptor, signature, exceptions);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            return super.visitField(
                    widener.widensField(internalName, name) ? publicised(access) : access,
                    name, descriptor, signature, value);
        }
    }
}
