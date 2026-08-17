package fr.d4emon.fenix.ember;

import java.util.List;
import java.util.ArrayList;
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

    /**
     * A trim pattern: the shape stamped onto a piece of armour.
     *
     * <p>The pattern says what is drawn; a trim material says what colour it is
     * drawn in, and the two are chosen separately at a smithing table. A mod
     * adding one without the other still works — its pattern can be applied in
     * every vanilla material.
     *
     * <p>The asset is a texture the mod ships under
     * {@code assets/&lt;mod&gt;/textures/trims/entity/humanoid/&lt;name&gt;.png},
     * with a matching {@code humanoid_leggings} copy. Both are greyscale: the
     * material recolours them, so a pattern drawn in colour comes out wrong in
     * every material but one.
     *
     * @param name the path part of its id, and the texture's name
     */
    protected final void trimPattern(String name) {
        save("trim_pattern", name, """
                {
                  "asset_id": %s,
                  "description": {
                    "translate": "trim_pattern.%s.%s"
                  },
                  "decal": false
                }
                """.formatted(EmberOutput.quote(modId() + ":" + name), modId(), name));
    }

    /**
     * A trim material: the colour a pattern is drawn in.
     *
     * <p>The asset name picks the palette, and it is a plain name rather than
     * an id — the game appends it to each pattern's texture path. So a material
     * named {@code ruby} needs every pattern it can be applied to, vanilla's
     * included, to have a {@code _ruby} palette entry; shipping the material
     * alone leaves it drawing as the pattern's default.
     *
     * @param name  the path part of its id
     * @param asset the palette's name
     */
    protected final void trimMaterial(String name, String asset) {
        save("trim_material", name, """
                {
                  "asset_name": %s,
                  "description": {
                    "translate": "trim_material.%s.%s"
                  }
                }
                """.formatted(EmberOutput.quote(asset), modId(), name));
    }

    /**
     * Starts a variant of one of the game's animals.
     *
     * <p>Since the variants became data, a mod can add a cow, pig or chicken
     * that looks different without touching the entity at all. What it cannot
     * do is make one spawn without saying where: a variant with no spawn
     * conditions is legal, loads, and is only ever seen if something asks for
     * it by name.
     *
     * @param animal one of {@code cow}, {@code pig}, {@code chicken}
     * @param name   the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Variant variant(String animal, String name) {
        return new Variant(this, animal, name);
    }

    /** Collects one animal variant. */
    public static final class Variant {

        private final EmberCosmeticsProvider provider;
        private final String animal;
        private final String name;
        private final List<String> conditions = new ArrayList<>();

        private String model = "normal";
        private String texture;
        private String babyTexture;

        private Variant(EmberCosmeticsProvider provider, String animal, String name) {
            this.provider = provider;
            this.animal = animal;
            this.name = name;
        }

        /**
         * Which of the animal's shapes to use.
         *
         * <p>A cow is {@code normal}, {@code cold} or {@code warm}, and the
         * three are different models rather than different textures — a texture
         * drawn for one is unwrapped wrongly on the others.
         *
         * @param value the model's name
         * @return this builder
         */
        public Variant model(String value) {
            this.model = value;
            return this;
        }

        /**
         * @param id the texture, such as {@code example-mod:entity/cow/ruby}
         * @return this builder
         */
        public Variant texture(String id) {
            this.texture = id;
            return this;
        }

        /**
         * @param id the texture worn by the baby, which is a separate file
         * @return this builder
         */
        public Variant babyTexture(String id) {
            this.babyTexture = id;
            return this;
        }

        /**
         * Spawns this variant in a biome.
         *
         * @param biome    a biome id, or a {@code #tag}
         * @param priority higher wins where two variants both apply
         * @return this builder
         */
        public Variant inBiome(String biome, int priority) {
            // A holder set, which is a tag id on its own or a list of ids —
            // never a single bare id. The codec's message for getting this
            // wrong is "Not a tag id", which names the branch it tried last
            // rather than what was expected.
            String biomes = biome.startsWith("#")
                    ? EmberOutput.quote(biome)
                    : "[" + EmberOutput.quote(biome) + "]";
            conditions.add("""
                    {
                          "priority": %d,
                          "condition": {
                            "type": "minecraft:biome",
                            "biomes": %s
                          }
                        }""".formatted(priority, biomes));
            return this;
        }

        /** Writes the variant. */
        public void save() {
            if (texture == null) {
                throw new IllegalStateException(
                        name + " has no texture, so it is the default one under another name");
            }
            String baby = babyTexture == null ? texture : babyTexture;
            provider.save(animal + "_variant", name, """
                    {
                      "model": %s,
                      "asset_id": %s,
                      "baby_asset_id": %s,
                      "spawn_conditions": [
                        %s
                      ]
                    }
                    """.formatted(EmberOutput.quote(model), EmberOutput.quote(texture),
                    EmberOutput.quote(baby), String.join(",\n    ", conditions)));
        }
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
