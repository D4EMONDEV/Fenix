package fr.d4emon.fenix.doclet;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Writes the API reference as pages the website renders.
 *
 * <p>A doclet rather than a parser: javadoc has already resolved every type and
 * every comment by the time this runs, so nothing here has to guess what a name
 * refers to or where a comment ends. The alternative — reading the HTML javadoc
 * produces, or the sources directly — breaks on the first thing it did not
 * expect.
 *
 * <p>One page per package rather than per class. A hundred and fifty pages in a
 * sidebar is not a reference anyone browses; fifteen, each holding a package's
 * classes under their own headings, is — and a class still gets a link of its
 * own through its heading anchor.
 */
public final class MarkdownDoclet implements Doclet {

    private Reporter reporter;
    private Path output;
    private String title = "API reference";

    /** Instantiated by javadoc. */
    public MarkdownDoclet() {
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return "Markdown";
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(
                new SimpleOption("-d", "Where to write the pages", "<directory>",
                        value -> output = Path.of(value)),
                new SimpleOption("--doctitle", "The heading the index carries", "<text>",
                        value -> title = value),
                // Passed by Gradle's Javadoc task whether or not a doclet wants
                // them. Accepted and ignored rather than fatal.
                new SimpleOption("-doctitle", "Ignored", "<text>", value -> { }),
                new SimpleOption("-windowtitle", "Ignored", "<text>", value -> { }),
                new SimpleOption("-notimestamp", "Ignored", null, value -> { }),
                new SimpleOption("-quiet", "Ignored", null, value -> { }));
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        if (output == null) {
            reporter.print(Diagnostic.Kind.ERROR, "-d is required");
            return false;
        }
        try {
            return write(environment);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean write(DocletEnvironment environment) throws IOException {
        DocTrees trees = environment.getDocTrees();
        Map<PackageElement, List<TypeElement>> byPackage = new TreeMap<>(
                Comparator.comparing(p -> p.getQualifiedName().toString()));

        for (Element element : environment.getIncludedElements()) {
            if (element instanceof TypeElement type && isDocumented(type)) {
                byPackage.computeIfAbsent(packageOf(type), key -> new ArrayList<>()).add(type);
            }
        }
        if (byPackage.isEmpty()) {
            reporter.print(Diagnostic.Kind.WARNING, "no types to document");
            return true;
        }

        Files.createDirectories(output);
        Map<String, String> index = new LinkedHashMap<>();

        for (var entry : byPackage.entrySet()) {
            PackageElement pkg = entry.getKey();
            List<TypeElement> types = entry.getValue();
            types.sort(Comparator.comparing(MarkdownDoclet::displayName));

            String name = pkg.getQualifiedName().toString();
            String slug = name.replace('.', '-');
            index.put(name, slug);

            Files.writeString(output.resolve(slug + ".md"),
                    packagePage(trees, pkg, types), StandardCharsets.UTF_8);
        }

        Files.writeString(output.resolve("index.md"), indexPage(index), StandardCharsets.UTF_8);
        reporter.print(Diagnostic.Kind.NOTE,
                "wrote " + (index.size() + 1) + " reference pages to " + output);
        return true;
    }

    private String packagePage(DocTrees trees, PackageElement pkg, List<TypeElement> types) {
        StringBuilder out = new StringBuilder();
        String name = pkg.getQualifiedName().toString();

        DocCommentTree comment = trees.getDocCommentTree(pkg);
        String summary = comment == null ? "Types in " + name
                : DocRenderer.summary(comment.getFirstSentence());

        out.append("---\n")
                .append("title: ").append(quote(shortPackage(name))).append('\n')
                .append("description: ").append(quote(summary)).append('\n')
                .append("sidebar:\n  order: ").append(order(name)).append('\n')
                // Types only in the table of contents. A package's methods are
                // its third-level headings, and twenty signatures down the side
                // of the page is a wall rather than a way through it.
                .append("tableOfContents:\n  maxHeadingLevel: 2\n")
                .append("---\n\n")
                .append("<p class=\"api-package\"><code>").append(name).append("</code></p>\n\n");

        if (comment != null && !comment.getFullBody().isEmpty()) {
            out.append(DocRenderer.render(comment.getFullBody())).append("\n\n");
        }

        // A table first: what is in here, at a glance, before the detail.
        out.append("| Type | What it is |\n|---|---|\n");
        for (TypeElement type : types) {
            DocCommentTree doc = trees.getDocCommentTree(type);
            out.append("| [`").append(displayName(type)).append("`](#")
                    .append(anchor(displayName(type))).append(") | ")
                    .append(doc == null ? "" : DocRenderer.summary(doc.getFirstSentence()))
                    .append(" |\n");
        }
        out.append('\n');

        for (TypeElement type : types) {
            appendType(out, trees, type);
        }
        return out.toString();
    }

    private void appendType(StringBuilder out, DocTrees trees, TypeElement type) {
        out.append("## ").append(displayName(type)).append("\n\n");

        DocCommentTree doc = trees.getDocCommentTree(type);
        if (doc != null && !doc.getFullBody().isEmpty()) {
            out.append(DocRenderer.render(doc.getFullBody())).append("\n\n");
        }

        List<VariableElement> constants = new ArrayList<>();
        List<ExecutableElement> methods = new ArrayList<>();
        for (Element member : type.getEnclosedElements()) {
            if (!isDocumented(member)) {
                continue;
            }
            if (member instanceof VariableElement field
                    && field.getModifiers().contains(Modifier.STATIC)) {
                constants.add(field);
            } else if (member instanceof ExecutableElement method
                    && method.getKind() == ElementKind.METHOD) {
                methods.add(method);
            }
        }

        if (!constants.isEmpty()) {
            out.append("**Constants**\n\n| Name | What it is |\n|---|---|\n");
            for (VariableElement field : constants) {
                DocCommentTree fieldDoc = trees.getDocCommentTree(field);
                out.append("| `").append(field.getSimpleName()).append("` | ")
                        .append(fieldDoc == null ? "" : DocRenderer.summary(fieldDoc.getFirstSentence()))
                        .append(" |\n");
            }
            out.append('\n');
        }

        methods.sort(Comparator.comparing(m -> m.getSimpleName().toString()));
        for (ExecutableElement method : methods) {
            out.append("### ").append(signature(method)).append("\n\n");
            DocCommentTree methodDoc = trees.getDocCommentTree(method);
            if (methodDoc != null && !methodDoc.getFullBody().isEmpty()) {
                out.append(DocRenderer.render(methodDoc.getFullBody())).append("\n\n");
            }
        }
    }

    private String indexPage(Map<String, String> packages) {
        StringBuilder out = new StringBuilder();
        out.append("---\n")
                .append("title: ").append(quote(title)).append('\n')
                .append("description: ")
                .append(quote("Every package a mod is written against, generated from the source."))
                .append('\n')
                .append("sidebar:\n  order: 0\n")
                .append("---\n\n")
                .append("Generated from the javadoc in the source, so what is written here is what\n")
                .append("the compiler sees. The client halves are included: `KeyBindings`,\n")
                .append("`MenuScreens` and `EntityRendering` live there.\n\n")
                .append("| Package | |\n|---|---|\n");
        packages.forEach((name, slug) -> out.append("| [`").append(name).append("`](")
                .append("./").append(slug).append("/) | ").append(shortPackage(name)).append(" |\n"));
        return out.toString();
    }

    /**
     * {@return the package a type belongs to}
     *
     * <p>Walked rather than cast: a nested type's enclosing element is the type
     * around it, not a package, and casting one to the other is a crash rather
     * than a missing entry.
     */
    private static PackageElement packageOf(TypeElement type) {
        Element enclosing = type;
        while (!(enclosing instanceof PackageElement pkg)) {
            enclosing = enclosing.getEnclosingElement();
        }
        return pkg;
    }

    /**
     * {@return a type's name as a reader would write it}
     *
     * <p>Qualified by its outer type when it has one: {@code Context} on its
     * own says nothing, and there are several.
     */
    private static String displayName(TypeElement type) {
        StringBuilder name = new StringBuilder(type.getSimpleName());
        Element enclosing = type.getEnclosingElement();
        while (enclosing instanceof TypeElement outer) {
            name.insert(0, outer.getSimpleName() + ".");
            enclosing = outer.getEnclosingElement();
        }
        return name.toString();
    }

    /**
     * The order packages are read in, rather than the order they sort in.
     *
     * <p>Alphabetical would open the reference on {@code api.log} and bury the
     * registry between the network and the resources. This is roughly the order
     * a mod meets them: what a mod <em>is</em>, then what it adds, then how it
     * reacts, then everything it talks to, and the loader last — which is the
     * one package most readers never need.
     */
    private static final List<String> READING_ORDER = List.of(
            "fr.d4emon.fenix.api",
            "fr.d4emon.fenix.registry",
            "fr.d4emon.fenix.event",
            "fr.d4emon.fenix.network",
            "fr.d4emon.fenix.command",
            "fr.d4emon.fenix.config",
            "fr.d4emon.fenix.resource",
            "fr.d4emon.fenix.ember",
            "fr.d4emon.fenix.loader");

    private static int order(String packageName) {
        for (int i = 0; i < READING_ORDER.size(); i++) {
            if (packageName.startsWith(READING_ORDER.get(i))) {
                // Ten apart, so a sub-package sorts under its parent rather
                // than ahead of the next group.
                return (i + 1) * 10 + (packageName.equals(READING_ORDER.get(i)) ? 0 : 1);
            }
        }
        return 999;
    }

    /** {@return the readable half of a package name, for a page title} */
    private static String shortPackage(String name) {
        String trimmed = name.startsWith("fr.d4emon.fenix.")
                ? name.substring("fr.d4emon.fenix.".length())
                : name;
        return trimmed.isEmpty() ? name : trimmed;
    }

    /** {@return a method rendered the way its declaration reads} */
    private static String signature(ExecutableElement method) {
        StringBuilder out = new StringBuilder("`");
        if (method.getModifiers().contains(Modifier.STATIC)) {
            out.append("static ");
        }
        out.append(simple(method.getReturnType().toString())).append(' ')
                .append(method.getSimpleName()).append('(');
        for (int i = 0; i < method.getParameters().size(); i++) {
            VariableElement parameter = method.getParameters().get(i);
            out.append(i == 0 ? "" : ", ")
                    .append(simple(parameter.asType().toString())).append(' ')
                    .append(parameter.getSimpleName());
        }
        return out.append(")`").toString();
    }

    /** Strips package names from a type, keeping generics readable. */
    private static String simple(String type) {
        // The lookbehind is what keeps GenerationStep.Decoration whole. Without
        // it the scan restarts inside the word, matches "enerationStep." as
        // though it were a package, and leaves "GDecoration".
        return type.replaceAll("(?<![A-Za-z0-9_$])[a-z][a-zA-Z0-9_]*\\.", "");
    }

    private static String anchor(String heading) {
        return heading.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private static String quote(String text) {
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    /**
     * {@return whether an element belongs in the reference}
     *
     * <p>Public and protected only. A mod cannot call anything else, and a
     * reference that lists what a reader cannot use wastes their time twice —
     * once reading it, once discovering it does not compile.
     */
    private static boolean isDocumented(Element element) {
        // Anything named fenix$ is public so that a mixin in another package can
        // reach it, not because a mod should. Listing it invites the call its
        // own javadoc asks the reader not to make.
        if (element.getSimpleName().toString().contains("fenix$")) {
            return false;
        }
        Set<Modifier> modifiers = element.getModifiers();
        return modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED);
    }

    /** A doclet option that takes one argument, or none. */
    private record SimpleOption(String name, String description, String parameter,
                                java.util.function.Consumer<String> action) implements Option {

        @Override
        public int getArgumentCount() {
            return parameter == null ? 0 : 1;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return List.of(name);
        }

        @Override
        public String getParameters() {
            return parameter == null ? "" : parameter;
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            action.accept(arguments.isEmpty() ? "" : arguments.get(0));
            return true;
        }
    }
}
