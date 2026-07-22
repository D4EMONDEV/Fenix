package fr.d4emon.fenix.loader.launch;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Puts a failed launch somewhere the person who caused it will find it.
 *
 * <p>Fenix already says what went wrong, and says it well — "duplicate mod
 * 'x': both a.jar and b.jar provide it, remove one of them" is the whole
 * answer. What it did with that answer was print it to standard output and
 * exit, which for anyone starting the game from the Minecraft launcher means
 * the window vanishes and nothing else happens. The diagnosis was fine; it went
 * nowhere they would look.
 *
 * <p>So it is written to a file first — a file survives, a window does not, and
 * it is what gets pasted into a bug report — and then shown, if there is a
 * screen to show it on.
 */
public final class FailureReport {

    /** Beside the game rather than inside {@code .fenix}: a hidden report is no report. */
    static final String FILE_NAME = "fenix-launch-error.txt";

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FailureReport() {
    }

    /**
     * Writes the failure down, and shows it when there is somewhere to show it.
     *
     * @param failure  what went wrong
     * @param diagnosed whether the message is the whole story — a resolution
     *                  problem is, an unexpected exception is not, and the two
     *                  deserve different amounts of stack trace
     * @param gameDir  where to write the report, if it is known yet
     */
    public static void publish(Throwable failure, boolean diagnosed, Path gameDir) {
        String text = compose(failure, diagnosed);

        // Always to the console: a dedicated server has no screen, and a
        // developer watching a terminal should not have to open a file.
        System.err.println();
        System.err.println(text);

        Optional<Path> written = write(text, gameDir);
        written.ifPresent(path -> System.err.println("This is also in " + path.toAbsolutePath()));

        show(text, written.orElse(null));
    }

    /** {@return the report, ready to be read by someone who did not write Fenix} */
    private static String compose(Throwable failure, boolean diagnosed) {
        StringBuilder out = new StringBuilder();
        out.append("Fenix could not start the game.\n")
                .append(LocalDateTime.now().format(WHEN)).append('\n')
                .append("Fenix ").append(FenixVersion.current()).append("\n\n");

        String message = failure.getMessage();
        out.append(message == null ? failure.toString() : message).append('\n');

        // A diagnosed failure has already said everything useful; its cause is
        // context. An undiagnosed one is a bug, and the stack is the report.
        Throwable stack = diagnosed ? failure.getCause() : failure;
        if (stack != null) {
            StringWriter trace = new StringWriter();
            stack.printStackTrace(new PrintWriter(trace));
            out.append('\n').append(trace);
        }
        return out.toString();
    }

    private static Optional<Path> write(String text, Path gameDir) {
        Path target = (gameDir == null ? Path.of(".") : gameDir).resolve(FILE_NAME);
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.writeString(target, text, StandardCharsets.UTF_8);
            return Optional.of(target);
        } catch (IOException | RuntimeException unwritable) {
            // The console copy above is the report now. Failing to report a
            // failure by failing again helps nobody.
            return Optional.empty();
        }
    }

    /**
     * Shows the report in a window, when the machine has one.
     *
     * <p>Reflection rather than an import: a dedicated server may run on a JDK
     * built without {@code java.desktop}, and a loader that cannot start there
     * because it wanted to draw an error box would be worse than the error.
     */
    private static void show(String text, Path written) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            Class<?> pane = Class.forName("javax.swing.JOptionPane");
            Object message = written == null ? text
                    : text + "\nSaved to " + written.toAbsolutePath();

            pane.getMethod("showMessageDialog", Class.forName("java.awt.Component"),
                            Object.class, String.class, int.class)
                    .invoke(null, null, scrollable(message.toString()),
                            "Fenix could not start", /* ERROR_MESSAGE */ 0);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError noWindow) {
            // No desktop, no display, no window. The file and the console stand.
        }
    }

    /**
     * {@return the report in something that scrolls}
     *
     * <p>A plain string dialog grows to whatever it holds, and a report naming
     * nine mods grows off the screen — taking its dismiss button with it.
     */
    private static Object scrollable(String text) throws ReflectiveOperationException {
        Class<?> textArea = Class.forName("javax.swing.JTextArea");
        Object area = textArea.getConstructor(String.class, int.class, int.class)
                .newInstance(text, 20, 90);
        textArea.getMethod("setEditable", boolean.class).invoke(area, false);
        textArea.getMethod("setCaretPosition", int.class).invoke(area, 0);

        return Class.forName("javax.swing.JScrollPane")
                .getConstructor(Class.forName("java.awt.Component"))
                .newInstance(area);
    }
}
