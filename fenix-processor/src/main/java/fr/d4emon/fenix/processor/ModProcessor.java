package fr.d4emon.fenix.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Turns {@code @Mod} annotations into the index the loader reads at startup.
 *
 * <p>This is why a Fenix mod's metadata never points at code: the declaration
 * is the annotation, on the class, and this processor records it while the mod
 * compiles. Anything that would otherwise fail at launch — an abstract class, a
 * missing public no-arg constructor, a class that does not implement
 * {@code FenixMod} — is rejected here instead, as a compiler error with a line
 * number.
 *
 * <p>The processor is deliberately dependency-free, including on the Fenix API:
 * it runs inside the mod author's compiler, and anything on its classpath lands
 * on theirs. Annotations are matched by fully qualified name.
 */
@SupportedAnnotationTypes({ModProcessor.MOD_ANNOTATION, ModProcessor.GENERATOR_ANNOTATION})
@SupportedOptions(ModProcessor.INDEX_FILE_OPTION)
public final class ModProcessor extends AbstractProcessor {

    static final String MOD_ANNOTATION = "fr.d4emon.fenix.api.Mod";
    static final String FENIX_MOD_INTERFACE = "fr.d4emon.fenix.api.FenixMod";

    static final String GENERATOR_ANNOTATION = "fr.d4emon.fenix.ember.Generator";
    static final String GENERATOR_INTERFACE = "fr.d4emon.fenix.ember.EmberGenerator";

    /** Written to the jar root; read by the loader's {@code ModIndexReader}. */
    static final String INDEX_FILE = "fenix.index.json";

    /**
     * Names the index file, so a second compilation in the same jar does not
     * overwrite the first.
     *
     * <p>Set by the Gradle plugin for the client source set, whose entry class
     * has to be indexed separately — the server must never be told to load it.
     */
    static final String INDEX_FILE_OPTION = "fenix.indexFile";

    /** Mirrors the rule in {@code fr.d4emon.fenix.api.ModInfo}. */
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9-]{1,63}");

    /** Mod id to binary class name, sorted so the output is reproducible. */
    private final Map<String, String> mods = new TreeMap<>();

    /** Binary names of the {@code @Generator} classes, likewise sorted. */
    private final java.util.SortedSet<String> generators = new java.util.TreeSet<>();

    private boolean failed;

    /** Instantiated by the compiler, which discovers it through {@code META-INF/services}. */
    public ModProcessor() {
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // The processor inspects nothing version-specific, so it should not
        // warn on every future JDK.
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            boolean isGenerator = annotation.getQualifiedName().contentEquals(GENERATOR_ANNOTATION);
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (isGenerator) {
                    considerGenerator(element);
                } else {
                    consider(element);
                }
            }
        }
        if (roundEnv.processingOver() && !failed && !mods.isEmpty()) {
            writeIndex();
        }
        // Leave @Mod visible to other processors.
        return false;
    }

    private void consider(Element element) {
        if (element.getKind() != ElementKind.CLASS && element.getKind() != ElementKind.RECORD) {
            error(element, "@Mod can only mark a class");
            return;
        }
        TypeElement type = (TypeElement) element;
        String name = type.getQualifiedName().toString();

        if (type.getModifiers().contains(Modifier.ABSTRACT)) {
            error(type, "@Mod class " + name + " must not be abstract — Fenix has to instantiate it");
        }
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            error(type, "@Mod class " + name + " must be public so Fenix can instantiate it");
        }
        if (type.getNestingKind() != NestingKind.TOP_LEVEL
                && !(type.getNestingKind() == NestingKind.MEMBER && type.getModifiers().contains(Modifier.STATIC))) {
            error(type, "@Mod class " + name + " must be a top-level or static nested class — "
                    + "a non-static inner class cannot be instantiated on its own");
        }
        if (!hasPublicNoArgConstructor(type)) {
            error(type, "@Mod class " + name + " needs a public no-argument constructor");
        }
        checkImplements(type, name, FENIX_MOD_INTERFACE, "@Mod");

        String id = readId(type);
        if (id == null) {
            // The compiler already reports the missing value; nothing useful to add.
            failed = true;
            return;
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            error(type, "'" + id + "' is not a valid mod id (expected 2 to 64 characters: "
                    + "lowercase letters, digits and hyphens, starting with a letter)");
            return;
        }

        String binaryName = processingEnv.getElementUtils().getBinaryName(type).toString();
        String existing = mods.putIfAbsent(id, binaryName);
        if (existing != null && !existing.equals(binaryName)) {
            error(type, "duplicate mod id '" + id + "': both " + existing + " and " + binaryName + " declare it");
        }
    }

    /**
     * Records a {@code @Generator} class, rejecting the same mistakes as for a
     * mod — Ember has to instantiate it just the same.
     */
    private void considerGenerator(Element element) {
        if (element.getKind() != ElementKind.CLASS && element.getKind() != ElementKind.RECORD) {
            error(element, "@Generator can only mark a class");
            return;
        }
        TypeElement type = (TypeElement) element;
        String name = type.getQualifiedName().toString();

        if (type.getModifiers().contains(Modifier.ABSTRACT)) {
            error(type, "@Generator class " + name + " must not be abstract — Ember has to instantiate it");
        }
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            error(type, "@Generator class " + name + " must be public so Ember can instantiate it");
        }
        if (type.getNestingKind() != NestingKind.TOP_LEVEL
                && !(type.getNestingKind() == NestingKind.MEMBER && type.getModifiers().contains(Modifier.STATIC))) {
            error(type, "@Generator class " + name + " must be a top-level or static nested class");
        }
        if (!hasPublicNoArgConstructor(type)) {
            error(type, "@Generator class " + name + " needs a public no-argument constructor");
        }
        checkImplements(type, name, GENERATOR_INTERFACE, "@Generator");
        if (!indexFile().equals(INDEX_FILE)) {
            // Ember reads only the common index, so a generator here would be
            // quietly skipped and its files never written. Refusing says the
            // one thing worth saying instead of leaving it to be discovered by
            // a missing model in game.
            error(type, "@Generator class " + name + " is in the client source set, where Ember "
                    + "never looks. Generators describe files against registered content, and their "
                    + "output lands in src/main/generated — move it to src/main/java");
        }

        generators.add(processingEnv.getElementUtils().getBinaryName(type).toString());
    }

    /**
     * Checks that an annotated class implements what its annotation promises.
     *
     * @param type           the annotated class
     * @param name           its qualified name, for the message
     * @param interfaceName  the interface it must implement
     * @param annotationName how to refer to the annotation in the message
     */
    private void checkImplements(TypeElement type, String name, String interfaceName, String annotationName) {
        TypeElement required = processingEnv.getElementUtils().getTypeElement(interfaceName);
        if (required == null) {
            error(type, interfaceName + " is not on the compile classpath");
            return;
        }
        if (!processingEnv.getTypeUtils().isAssignable(type.asType(), required.asType())) {
            error(type, annotationName + " class " + name + " does not implement " + interfaceName);
        }
    }

    private static boolean hasPublicNoArgConstructor(TypeElement type) {
        for (ExecutableElement constructor : ElementFilter.constructorsIn(type.getEnclosedElements())) {
            if (constructor.getParameters().isEmpty() && constructor.getModifiers().contains(Modifier.PUBLIC)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads the annotation's {@code value} by name — the processor cannot
     * reference the {@code Mod} class itself without depending on the API.
     */
    private static String readId(TypeElement type) {
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            if (!mirror.getAnnotationType().toString().equals(MOD_ANNOTATION)) {
                continue;
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                    : mirror.getElementValues().entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals("value")
                        && entry.getValue().getValue() instanceof String id) {
                    return id;
                }
            }
        }
        return null;
    }

    private void writeIndex() {
        StringBuilder json = new StringBuilder(64 + mods.size() * 64);
        json.append("{\n  \"schema\": 1,\n  \"mods\": {");
        String separator = "\n    ";
        for (Map.Entry<String, String> entry : mods.entrySet()) {
            json.append(separator).append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
            separator = ",\n    ";
        }
        json.append("\n  }");

        // Ember reads this to find what generates a mod's resources, so nothing
        // has to name a class in a build file.
        if (!generators.isEmpty()) {
            json.append(",\n  \"generators\": [");
            String generatorSeparator = "\n    ";
            for (String generator : generators) {
                json.append(generatorSeparator).append(quote(generator));
                generatorSeparator = ",\n    ";
            }
            json.append("\n  ]");
        }
        json.append("\n}\n");

        try (Writer writer = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", indexFile())
                .openWriter()) {
            writer.write(json.toString());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "could not write " + indexFile() + ": " + e.getMessage());
        }
    }

    /** {@return the file to write, {@value #INDEX_FILE} unless told otherwise} */
    private String indexFile() {
        return processingEnv.getOptions().getOrDefault(INDEX_FILE_OPTION, INDEX_FILE);
    }

    /**
     * Minimal JSON string escaping. Ids and class names cannot contain anything
     * exotic, but a hand-rolled writer has no excuse for producing broken JSON.
     */
    private static String quote(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2).append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private void error(Element element, String message) {
        failed = true;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
