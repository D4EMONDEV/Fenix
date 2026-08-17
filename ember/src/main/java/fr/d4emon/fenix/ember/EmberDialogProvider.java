package fr.d4emon.fenix.ember;

import com.google.gson.JsonParser;
import net.minecraft.server.dialog.Dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes dialogs — the screens a server can open on a player's client without
 * the client knowing anything about them.
 *
 * <p>New in this line of the game, and the first thing a mod has ever been able
 * to put on screen from the server side alone. Everything else that draws is
 * client code: a screen class, a renderer, a mixin. A dialog is a datapack
 * file, so it works on a vanilla client connected to a modded server.
 *
 * <pre>{@code
 * @Generator
 * public final class ModDialogs extends EmberDialogProvider {
 *
 *     @Override
 *     protected void dialogs() {
 *         notice("welcome")
 *                 .title("The Ruby Caverns")
 *                 .body("Something glitters below.")
 *                 .button("Onwards")
 *                 .save();
 *     }
 * }
 * }</pre>
 *
 * <p>Open one with {@code ServerPlayer.openDialog(holder)}, or from a command
 * with {@code /dialog show}.
 */
public abstract class EmberDialogProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberDialogProvider() {
    }

    /** Describes the dialogs. */
    protected abstract void dialogs();

    @Override
    protected final void run() {
        dialogs();
    }

    /**
     * Starts a notice: a title, some text, and one button that closes it.
     *
     * <p>The simplest of the five kinds, and the one that covers telling a
     * player something. The others — confirmation, multi-action, dialog list,
     * server links — take the same common fields and add their own.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Notice notice(String name) {
        return new Notice(this, name);
    }

    /** Collects one notice dialog. */
    public static final class Notice {

        private final EmberDialogProvider provider;
        private final String name;
        private final List<String> body = new ArrayList<>();

        private String title = "";
        private String label = "Ok";
        private boolean pause = true;
        private boolean escape = true;

        private Notice(EmberDialogProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * @param text the heading, and what the screen is called
         * @return this builder
         */
        public Notice title(String text) {
            this.title = text;
            return this;
        }

        /**
         * Adds a paragraph. Call it more than once for more than one.
         *
         * @param text the paragraph
         * @return this builder
         */
        public Notice body(String text) {
            body.add("""
                    {
                          "type": "minecraft:plain_message",
                          "contents": %s
                        }""".formatted(EmberOutput.quote(text)));
            return this;
        }

        /**
         * @param text what the button says
         * @return this builder
         */
        public Notice button(String text) {
            this.label = text;
            return this;
        }

        /**
         * Lets the game keep running behind the dialog.
         *
         * <p>A dialog pauses a single-player game by default, which is right
         * for something a player must read and wrong for something shown while
         * they are being chased.
         *
         * @return this builder
         */
        public Notice withoutPausing() {
            this.pause = false;
            return this;
        }

        /**
         * Refuses to close on escape, so the button is the only way out.
         *
         * @return this builder
         */
        public Notice mustAnswer() {
            this.escape = false;
            return this;
        }

        /** Writes the dialog. */
        public void save() {
            String json = """
                    {
                      "type": "minecraft:notice",
                      "title": %s,
                      "can_close_with_escape": %b,
                      "pause": %b,
                      "body": [
                        %s
                      ],
                      "action": {
                        "label": %s
                      }
                    }
                    """.formatted(EmberOutput.quote(title), escape, pause,
                    String.join(",\n    ", body), EmberOutput.quote(label));

            // Read back with the game's own codec. A dialog that does not parse
            // is not opened at all — openDialog is handed a holder that was
            // never bound — so the failure arrives as a screen that does not
            // appear, which is indistinguishable from code that never ran.
            Dialog.DIRECT_CODEC.parse(registryOps(), JsonParser.parseString(json))
                    .getOrThrow(message -> new IllegalStateException(
                            "dialog " + name + " would not load: " + message));

            provider.output().data("dialog/" + name + ".json", json);
        }
    }
}
