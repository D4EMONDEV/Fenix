package fr.d4emon.fenix.gradle;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code accessible} declarations to a jar, so a mod can compile
 * against what the loader will open at run time.
 *
 * <p>Deliberately a second implementation of what the loader does rather than a
 * shared one: the plugin runs in Gradle, where the loader has no business being
 * on the classpath, and the alternative is a module that exists only to be
 * depended on from both. The rule being applied is four lines of bit twiddling;
 * what matters is that both read the same declarations out of the same file.
 */
final class Wideners {

    private static final int VISIBILITY = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED
            | Opcodes.ACC_PRIVATE;

    private final Set<String> classes = new LinkedHashSet<>();
    private final Map<String, Set<String>> methods = new LinkedHashMap<>();
    private final Map<String, Set<String>> fields = new LinkedHashMap<>();

    /**
     * @param declarations the {@code accessible} entries, as written in the manifest
     */
    Wideners(List<String> declarations) {
        for (String declaration : declarations) {
            String[] parts = declaration.trim().split("\\s+");
            switch (parts[0]) {
                case "class" -> classes.add(internal(parts[1]));
                case "method" -> methods.computeIfAbsent(internal(parts[1]),
                        key -> new LinkedHashSet<>()).add(parts[2]);
                case "field" -> fields.computeIfAbsent(internal(parts[1]),
                        key -> new LinkedHashSet<>()).add(parts[2]);
                default -> throw new IllegalArgumentException(
                        "'" + declaration + "' should start with class, method or field");
            }
        }
    }

    /**
     * {@return the class, widened if anything named it}
     *
     * @param entry the jar entry name
     * @param bytes the class file
     */
    byte[] apply(String entry, byte[] bytes) {
        if (classes.isEmpty() && methods.isEmpty() && fields.isEmpty()) {
            return bytes;
        }
        if (!entry.endsWith(".class")) {
            return bytes;
        }
        String name = entry.substring(0, entry.length() - ".class".length());
        if (!touches(name)) {
            return bytes;
        }
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new Widening(writer, name), 0);
        return writer.toByteArray();
    }

    /** A nested type is nameable only if its outer class says so as well. */
    private boolean touches(String name) {
        return classes.contains(name) || methods.containsKey(name) || fields.containsKey(name)
                || classes.stream().anyMatch(widened -> widened.startsWith(name + "$"));
    }

    private static int publicised(int access) {
        return (access & ~VISIBILITY) | Opcodes.ACC_PUBLIC;
    }

    private static String internal(String name) {
        return name.replace('.', '/');
    }

    private final class Widening extends ClassVisitor {

        private final String owner;

        Widening(ClassVisitor next, String owner) {
            super(Opcodes.ASM9, next);
            this.owner = owner;
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, classes.contains(name) ? publicised(access) : access,
                    name, signature, superName, interfaces);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            // The half that decides whether javac lets you name the type at all.
            super.visitInnerClass(name, outerName, innerName,
                    classes.contains(name) ? publicised(access) : access);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            boolean widen = methods.getOrDefault(owner, Set.of()).contains(name);
            return super.visitMethod(widen ? publicised(access) : access,
                    name, descriptor, signature, exceptions);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            boolean widen = fields.getOrDefault(owner, Set.of()).contains(name);
            return super.visitField(widen ? publicised(access) : access,
                    name, descriptor, signature, value);
        }
    }
}
