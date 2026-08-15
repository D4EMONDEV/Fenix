package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;

/**
 * Writes the small data kinds that are pure presentation: music discs,
 * paintings, goat horn sounds and banner patterns.
 *
 * <p>All four became data rather than code, all four are a handful of fields,
 * and all four are the kind of thing a mod adds several of at once — so they
 * share a provider rather than having four of their own.
 *
 * <pre>{@code
 * @Generator
 * public final class ModCosmetics extends EmberCosmeticsProvider {
 *     @Override
 *     protected void cosmetics() {
 *         jukeboxSong("ruby_waltz", ModContent.RUBY_WALTZ)
 *                 .description("Ruby Waltz").seconds(94).comparatorOutput(11).save();
 *
 *         painting("ruby_vein").size(2, 1)
 *                 .title("Ruby Vein").author("D4EMON").save();
 *     }
 * }
 * }</pre>
 */
public abstract class EmberCosmeticsProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberCosmeticsProvider() {
    }

    /** Describes them. */
    protected abstract void cosmetics();

    @Override
    protected final void run() {
        cosmetics();
    }

    /**
     * Starts a music disc's song.
     *
     * <p>The song is half of a disc: the other half is an item carrying the
     * {@code jukebox_playable} component that names this song.
     *
     * @param name  the path part of its id
     * @param sound the sound event to play, registered through the registrar
     * @return a builder; call {@code save()} when done
     */
    protected final Song jukeboxSong(String name, Holder<?> sound) {
        return new Song(this, name, EmberOutput.idOf(sound.get()).toString());
    }

    /**
     * Starts a music disc's song, naming the sound by id.
     *
     * @param name  the path part of its id
     * @param sound the sound event's id
     * @return a builder; call {@code save()} when done
     */
    protected final Song jukeboxSong(String name, String sound) {
        return new Song(this, name, sound);
    }

    /**
     * Starts a painting.
     *
     * <p>Needs a texture at {@code textures/painting/<name>.png}, sized to the
     * blocks it covers: 16 pixels per block each way.
     *
     * @param name the path part of its id, and the texture it looks for
     * @return a builder; call {@code save()} when done
     */
    protected final Painting painting(String name) {
        return new Painting(this, name);
    }

    /**
     * Starts a goat horn sound.
     *
     * @param name  the path part of its id
     * @param sound the sound event's id
     * @return a builder; call {@code save()} when done
     */
    protected final Instrument instrument(String name, String sound) {
        return new Instrument(this, name, sound);
    }

    /**
     * Writes a banner pattern.
     *
     * <p>Needs a texture at {@code textures/entity/banner/<asset>.png}, and a
     * line in the language file under {@code block.<namespace>.banner.<name>}.
     *
     * @param name the path part of its id, and the texture it looks for
     */
    protected final void bannerPattern(String name) {
        output().data("banner_pattern/" + name + ".json", """
                {
                  "asset_id": "%s:%s",
                  "translation_key": "block.%s.banner.%s"
                }
                """.formatted(modId(), name, modId(), name));
    }

    private void save(String directory, String name, String json) {
        output().data(directory + "/" + name + ".json", json);
    }

    /** Collects one music disc song. */
    public static final class Song {

        private final EmberCosmeticsProvider provider;
        private final String name;
        private final String sound;
        private String description;
        private float seconds = 60;
        private int comparatorOutput = 1;

        private Song(EmberCosmeticsProvider provider, String name, String sound) {
            this.provider = provider;
            this.name = name;
            this.sound = sound;
        }

        /**
         * @param text what the disc's tooltip says it is
         * @return this builder
         */
        public Song description(String text) {
            this.description = text;
            return this;
        }

        /**
         * How long the track runs.
         *
         * <p>Wrong, and the jukebox stops early or sits silent at the end —
         * the game trusts this number rather than the sound file.
         *
         * @param length the length in seconds
         * @return this builder
         */
        public Song seconds(float length) {
            this.seconds = length;
            return this;
        }

        /**
         * @param level what a comparator beside the jukebox reads, 1 to 15
         * @return this builder
         */
        public Song comparatorOutput(int level) {
            this.comparatorOutput = level;
            return this;
        }

        /** Writes the song. */
        public void save() {
            provider.save("jukebox_song", name, """
                    {
                      "comparator_output": %d,
                      "description": {
                        "text": %s
                      },
                      "length_in_seconds": %s,
                      "sound_event": %s
                    }
                    """.formatted(comparatorOutput,
                    EmberOutput.quote(description == null ? name : description),
                    EmberOutput.decimal(seconds), EmberOutput.quote(sound)));
        }
    }

    /** Collects one painting. */
    public static final class Painting {

        private final EmberCosmeticsProvider provider;
        private final String name;
        private String title;
        private String author;
        private int width = 1;
        private int height = 1;

        private Painting(EmberCosmeticsProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * @param blocksWide  how many blocks across
         * @param blocksHigh  how many blocks up
         * @return this builder
         */
        public Painting size(int blocksWide, int blocksHigh) {
            this.width = blocksWide;
            this.height = blocksHigh;
            return this;
        }

        /**
         * @param text the name shown in the painting's tooltip
         * @return this builder
         */
        public Painting title(String text) {
            this.title = text;
            return this;
        }

        /**
         * @param text who painted it
         * @return this builder
         */
        public Painting author(String text) {
            this.author = text;
            return this;
        }

        /** Writes the painting. */
        public void save() {
            StringBuilder json = new StringBuilder("{\n")
                    .append("  \"asset_id\": \"").append(provider.modId()).append(':')
                    .append(name).append("\",\n");
            if (author != null) {
                json.append("  \"author\": {\n    \"color\": \"gray\",\n    \"text\": ")
                        .append(EmberOutput.quote(author)).append("\n  },\n");
            }
            json.append("  \"height\": ").append(height).append(",\n");
            if (title != null) {
                json.append("  \"title\": {\n    \"color\": \"yellow\",\n    \"text\": ")
                        .append(EmberOutput.quote(title)).append("\n  },\n");
            }
            json.append("  \"width\": ").append(width).append("\n}\n");
            provider.save("painting_variant", name, json.toString());
        }
    }

    /** Collects one goat horn sound. */
    public static final class Instrument {

        private final EmberCosmeticsProvider provider;
        private final String name;
        private final String sound;
        private String description;
        private float range = 256;
        private float useSeconds = 7;

        private Instrument(EmberCosmeticsProvider provider, String name, String sound) {
            this.provider = provider;
            this.name = name;
            this.sound = sound;
        }

        /**
         * @param text what the horn's tooltip says
         * @return this builder
         */
        public Instrument description(String text) {
            this.description = text;
            return this;
        }

        /**
         * @param blocks how far it carries
         * @return this builder
         */
        public Instrument range(float blocks) {
            this.range = blocks;
            return this;
        }

        /**
         * @param seconds how long the player is held blowing it
         * @return this builder
         */
        public Instrument useSeconds(float seconds) {
            this.useSeconds = seconds;
            return this;
        }

        /** Writes the instrument. */
        public void save() {
            provider.save("instrument", name, """
                    {
                      "description": {
                        "text": %s
                      },
                      "range": %s,
                      "sound_event": %s,
                      "use_duration": %s
                    }
                    """.formatted(EmberOutput.quote(description == null ? name : description),
                    EmberOutput.decimal(range), EmberOutput.quote(sound),
                    EmberOutput.decimal(useSeconds)));
        }
    }
}
