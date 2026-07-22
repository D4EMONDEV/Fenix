package fr.d4emon.fenix.doclet;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;

import java.util.List;

/**
 * Turns a javadoc comment into what a Markdown page should hold.
 *
 * <p>Fenix's javadoc is prose, written with {@code <p>} and {@code <em>} and
 * the odd {@code <pre>} block. Markdown renders inline HTML, so most of it can
 * pass through untouched — which is the point: the documentation on the site is
 * the documentation in the source, not a paraphrase of it.
 *
 * <p>Three things are converted, because leaving them would look wrong.
 * {@code {@code …}} becomes a code span; a {@code <pre>} block becomes a fenced
 * one, so the site highlights it as Java; and a paragraph tag becomes a blank
 * line, because an unclosed one nests badly the moment a table follows it.
 */
final class DocRenderer {

    private DocRenderer() {
    }

    /**
     * {@return a comment rendered as Markdown}
     *
     * @param trees the comment's body, as javadoc parsed it
     */
    static String render(List<? extends DocTree> trees) {
        StringBuilder out = new StringBuilder();
        for (DocTree tree : trees) {
            switch (tree) {
                // {@code x} and {@literal x} both mean "show this verbatim".
                case LiteralTree literal -> code(out, literal.getBody().getBody());

                // {@link Foo#bar} names an API member. Rendering it as a link
                // would need every target's page to exist; a code span is at
                // least honest about being a name.
                case LinkTree link -> code(out, link.getReference() == null
                        ? "" : link.getReference().getSignature());

                default -> out.append(tree.toString());
            }
        }
        return paragraphs(fenceCodeBlocks(collapse(decodeEscapes(out.toString()))));
    }

    /** {@return the first sentence, flattened to one line for a table cell} */
    static String summary(List<? extends DocTree> firstSentence) {
        String text = render(firstSentence)
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", " ")
                .trim();
        // A pipe would end the cell it is written into.
        return text.replace("|", "\\|");
    }

    private static void code(StringBuilder out, String body) {
        String text = body.strip();
        if (text.isEmpty()) {
            return;
        }
        // A span holding a backtick needs a longer fence than the run it
        // contains, and one starting or ending with a backtick needs padding.
        String fence = text.contains("`") ? "``" : "`";
        String pad = text.startsWith("`") || text.endsWith("`") ? " " : "";
        out.append(fence).append(pad).append(text).append(pad).append(fence);
    }

    /**
     * Rewrites a {@code <pre>} block as a fenced Java block.
     *
     * <p>Javadoc's own form wraps {@code {@code …}} in {@code <pre>}, and the
     * inner part has already become a code span by the time this runs — so what
     * arrives is a {@code <pre>} around backticks. Both come off, a fence goes
     * on.
     */
    private static String fenceCodeBlocks(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int at = 0;
        while (true) {
            int open = text.indexOf("<pre>", at);
            int close = open < 0 ? -1 : text.indexOf("</pre>", open);
            if (open < 0 || close < 0) {
                return out.append(text, at, text.length()).toString();
            }
            out.append(text, at, open);

            String body = text.substring(open + "<pre>".length(), close).strip();
            if (body.startsWith("`") && body.endsWith("`")) {
                body = body.substring(1, body.length() - 1).strip();
            }
            out.append("\n\n```java\n").append(unescape(body)).append("\n```\n\n");
            at = close + "</pre>".length();
        }
    }

    /**
     * Turns javadoc's paragraph tags into blank lines.
     *
     * <p>Markdown renders inline HTML, so a page full of unclosed {@code <p>}
     * would work — right up to the first list or table after one, which would
     * end up inside it. A blank line says the same thing in the language the
     * page is written in.
     */
    private static String paragraphs(String text) {
        return squeeze(text.replaceAll("(?i)</?p>\\s*", "\n\n"));
    }

    /**
     * Decodes the {@code \\uXXXX} escapes javadoc hides non-ASCII behind.
     *
     * <p>Without this every em dash in Fenix's prose reaches the page as six
     * literal characters, and the page shows them.
     */
    private static String decodeEscapes(String text) {
        if (!text.contains("\\u")) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\\' && i + 5 < text.length() && text.charAt(i + 1) == 'u') {
                try {
                    out.append((char) Integer.parseInt(text.substring(i + 2, i + 6), 16));
                    i += 5;
                    continue;
                } catch (NumberFormatException notAnEscape) {
                    // Not an escape after all; keep the backslash as written.
                }
            }
            out.append(text.charAt(i));
        }
        return out.toString();
    }

    /** Undoes the entity escaping a javadoc comment requires. */
    private static String unescape(String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#64;", "@")
                .replace("&amp;", "&");
    }

    /** Trims the space every javadoc line carries after its asterisk. */
    private static String collapse(String text) {
        StringBuilder out = new StringBuilder(text.length());
        text.lines().forEach(line ->
                out.append(line.startsWith(" ") ? line.substring(1) : line).append('\n'));
        return squeeze(out.toString());
    }

    /** Collapses runs of blank lines, which Markdown reads as one break anyway. */
    private static String squeeze(String text) {
        return text.replaceAll("\n{3,}", "\n\n").strip();
    }
}
