package fr.d4emon.fenix.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compiles real sources through the JDK compiler with {@link ModProcessor}
 * attached — the same environment a mod author's build gives it.
 */
class ModProcessorTest {

    @TempDir
    Path outputDir;

    /** The jar or class directory holding the Fenix API, for the compile classpath. */
    private static final String API_CLASSPATH;

    static {
        try {
            API_CLASSPATH = Path.of(
                    fr.d4emon.fenix.api.Mod.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toString();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private record Result(boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics) {

        String messages() {
            StringBuilder all = new StringBuilder();
            for (Diagnostic<?> diagnostic : diagnostics) {
                all.append(diagnostic.getMessage(null)).append('\n');
            }
            return all.toString();
        }
    }

    private Result compile(Map<String, String> sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        List<JavaFileObject> files = new ArrayList<>();
        sources.forEach((className, code) -> files.add(new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        }));

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            fileManager.setLocation(StandardLocation.CLASS_PATH, List.of(new File(API_CLASSPATH)));

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, null, null, files);
            task.setProcessors(List.of(new ModProcessor()));
            boolean success = task.call();
            return new Result(success, diagnostics.getDiagnostics());
        }
    }

    private String indexContent() throws IOException {
        return Files.readString(outputDir.resolve("fenix.index.json"));
    }

    private static String validMod(String className, String id) {
        return """
                package com.example;

                import fr.d4emon.fenix.api.Fenix;
                import fr.d4emon.fenix.api.FenixMod;
                import fr.d4emon.fenix.api.Mod;

                @Mod("%s")
                public final class %s implements FenixMod {
                    @Override
                    public void onInit(Fenix fenix) {
                    }
                }
                """.formatted(id, className);
    }

    @Nested
    @DisplayName("a valid mod")
    class Valid {

        @Test
        void compilesAndLandsInTheIndex() throws IOException {
            Result result = compile(Map.of("com.example.DemoMod", validMod("DemoMod", "demo-mod")));

            assertTrue(result.success(), result.messages());
            String index = indexContent();
            assertTrue(index.contains("\"demo-mod\": \"com.example.DemoMod\""), index);
            assertTrue(index.contains("\"schema\": 1"), index);
        }

        @Test
        void indexesEveryModInTheCompilation() throws IOException {
            Result result = compile(Map.of(
                    "com.example.FirstMod", validMod("FirstMod", "first-mod"),
                    "com.example.SecondMod", validMod("SecondMod", "second-mod")));

            assertTrue(result.success(), result.messages());
            String index = indexContent();
            assertTrue(index.contains("first-mod"), index);
            assertTrue(index.contains("second-mod"), index);
        }

        @Test
        @DisplayName("a compilation without @Mod writes no index at all")
        void writesNothingWithoutMods() throws IOException {
            Result result = compile(Map.of("com.example.Plain", """
                    package com.example;

                    public final class Plain {
                    }
                    """));

            assertTrue(result.success(), result.messages());
            assertFalse(Files.exists(outputDir.resolve("fenix.index.json")));
        }

        @Test
        @DisplayName("a static nested class is instantiable, so it is accepted")
        void acceptsAStaticNestedClass() throws IOException {
            Result result = compile(Map.of("com.example.Outer", """
                    package com.example;

                    import fr.d4emon.fenix.api.FenixMod;
                    import fr.d4emon.fenix.api.Mod;

                    public final class Outer {
                        @Mod("nested-mod")
                        public static final class Nested implements FenixMod {
                        }
                    }
                    """));

            assertTrue(result.success(), result.messages());
            assertTrue(indexContent().contains("\"nested-mod\": \"com.example.Outer$Nested\""), indexContent());
        }
    }

    @Nested
    @DisplayName("what would crash at launch fails at compile time instead")
    class Rejected {

        private void assertRejected(String source, String expectedInMessage) throws IOException {
            Result result = compile(Map.of("com.example.BadMod", source));

            assertFalse(result.success(), "compilation should have failed");
            assertTrue(result.messages().contains(expectedInMessage), result.messages());
            assertFalse(Files.exists(outputDir.resolve("fenix.index.json")),
                    "no index should be written for a failed compilation");
        }

        @Test
        void anAbstractClass() throws IOException {
            assertRejected("""
                    package com.example;

                    import fr.d4emon.fenix.api.FenixMod;
                    import fr.d4emon.fenix.api.Mod;

                    @Mod("bad-mod")
                    public abstract class BadMod implements FenixMod {
                    }
                    """, "must not be abstract");
        }

        @Test
        void aClassNotImplementingFenixMod() throws IOException {
            assertRejected("""
                    package com.example;

                    import fr.d4emon.fenix.api.Mod;

                    @Mod("bad-mod")
                    public final class BadMod {
                    }
                    """, "does not implement fr.d4emon.fenix.api.FenixMod");
        }

        @Test
        void aMissingPublicNoArgConstructor() throws IOException {
            assertRejected("""
                    package com.example;

                    import fr.d4emon.fenix.api.FenixMod;
                    import fr.d4emon.fenix.api.Mod;

                    @Mod("bad-mod")
                    public final class BadMod implements FenixMod {
                        public BadMod(int value) {
                        }
                    }
                    """, "public no-argument constructor");
        }

        @Test
        void aPrivateConstructor() throws IOException {
            assertRejected("""
                    package com.example;

                    import fr.d4emon.fenix.api.FenixMod;
                    import fr.d4emon.fenix.api.Mod;

                    @Mod("bad-mod")
                    public final class BadMod implements FenixMod {
                        private BadMod() {
                        }
                    }
                    """, "public no-argument constructor");
        }

        @Test
        void aNonStaticInnerClass() throws IOException {
            assertRejected("""
                    package com.example;

                    import fr.d4emon.fenix.api.FenixMod;
                    import fr.d4emon.fenix.api.Mod;

                    public final class BadMod {
                        @Mod("inner-mod")
                        public final class Inner implements FenixMod {
                        }
                    }
                    """, "non-static inner class cannot be instantiated");
        }

        @Test
        void aNonPublicClass() throws IOException {
            assertRejected("""
                    package com.example;

                    import fr.d4emon.fenix.api.FenixMod;
                    import fr.d4emon.fenix.api.Mod;

                    @Mod("bad-mod")
                    final class BadMod implements FenixMod {
                    }
                    """, "must be public");
        }

        @Test
        void anInvalidId() throws IOException {
            assertRejected("""
                    package com.example;

                    import fr.d4emon.fenix.api.FenixMod;
                    import fr.d4emon.fenix.api.Mod;

                    @Mod("Bad_Id")
                    public final class BadMod implements FenixMod {
                    }
                    """, "not a valid mod id");
        }

        @Test
        void twoClassesClaimingTheSameId() throws IOException {
            Result result = compile(Map.of(
                    "com.example.FirstMod", validMod("FirstMod", "twin-mod"),
                    "com.example.SecondMod", validMod("SecondMod", "twin-mod")));

            assertFalse(result.success());
            assertTrue(result.messages().contains("duplicate mod id 'twin-mod'"), result.messages());
        }
    }
}
