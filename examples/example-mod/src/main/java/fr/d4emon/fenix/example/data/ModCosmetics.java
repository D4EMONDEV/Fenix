package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberCosmeticsProvider;
import fr.d4emon.fenix.ember.Generator;
import fr.d4emon.fenix.example.registry.ModContent;

/** The mod's presentation-only content: a song, a painting, a horn, a banner. */
@Generator
public final class ModCosmetics extends EmberCosmeticsProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModCosmetics() {
    }

    @Override
    protected void cosmetics() {
        // The song half of a music disc. The disc item itself carries a
        // jukebox_playable component naming this; without the item, the song
        // exists and nothing can play it.
        jukeboxSong("ruby_waltz", ModContent.RUBY_CHIME)
                .description("Ruby Waltz")
                .seconds(4)
                .comparatorOutput(11)
                .save();

        // Two blocks across, one up. The texture has to match: 32 by 16.
        painting("ruby_vein")
                .size(2, 1)
                .title("Ruby Vein")
                .author("D4EMON")
                .save();

        instrument("ruby_horn", "example-mod:ruby_chime")
                .description("Ruby Horn")
                .range(128)
                .useSeconds(5)
                .save();

        bannerPattern("ruby_facet");
    }
}
