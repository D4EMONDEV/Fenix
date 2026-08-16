package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberStructureProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * A small shrine of ruby, placed rarely on the surface.
 *
 * <p>The three files here are generated. The fourth — the {@code .nbt} template
 * they point at — is committed under {@code resources}, because nothing
 * generates one from Java: a template is normally made in game with a structure
 * block. This one was written byte by byte, which is possible and is not what
 * Ember is for.
 */
@Generator
public final class ModStructures extends EmberStructureProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModStructures() {
    }

    @Override
    protected void structures() {
        // What the world does to the shrine before a player ever sees it. A
        // template is stamped in exactly as it was saved, so without this the
        // shrine is always pristine — which reads as something built this
        // morning rather than something that was already here.
        processorList("weathered")
                .rot(0.9f)                     // one block in ten missing
                .mossy(0.2f)
                .replace("minecraft:stone_bricks",
                        "minecraft:cracked_stone_bricks", 0.3f)
                .save();

        templatePool("shrine")
                .piece("example-mod:ruby_shrine", 1, "example-mod:weathered")
                .save();

        structure("ruby_shrine")
                .startPool("example-mod:shrine")
                .biomes("#minecraft:is_overworld")
                .size(1)                       // the start piece and nothing more
                .terrainAdaptation("beard_thin")
                .save();

        // Rare: every 24 chunks on average, never closer than 8. The salt is
        // arbitrary and has to be a number no other set picked, or the two
        // generate in the same chunks forever.
        structureSet("ruby_shrines")
                .structure("example-mod:ruby_shrine")
                .spacing(24, 8)
                .salt(48213977)
                .save();
    }
}
