package fr.d4emon.fenix.probe;

import com.mojang.serialization.Lifecycle;
import fr.d4emon.fenix.resource.ModPackSource;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Runs as the game: asks what the packs Fenix hands to Minecraft actually
 * declare.
 *
 * <p>The question is whether a mod's datapack is a <em>known</em> pack, and it
 * decides something no part of it mentions. Vanilla settles the lifecycle of
 * every datapack registry entry by the pack it came from — no {@code KnownPack}
 * means {@code Lifecycle.experimental} — one experimental registry makes
 * {@code allRegistriesLifecycle()} experimental, and the create-world screen
 * warns on anything that is not stable. So a single worldgen file in a single
 * mod produced "Experimental Features Warning" on every new world, and nothing
 * in that message named a mod, a file or a pack.
 *
 * <p>This runs inside a real launch because {@code ModPackSource} reads the
 * mods from the running loader; there is nothing to inspect without one.
 */
public final class ResourceProbe {

    private ResourceProbe() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        List<Pack> packs = new ArrayList<>();
        new ModPackSource(PackType.SERVER_DATA).loadPacks(packs::add);

        require(!packs.isEmpty(),
                "a mod carrying data should be offered to the game as a datapack");

        for (Pack pack : packs) {
            Optional<KnownPack> known = pack.location().knownPackInfo();
            require(known.isPresent(),
                    pack.location().id() + " should declare a KnownPack — without one every "
                            + "datapack registry entry it carries is marked experimental, and every "
                            + "new world asks the player to confirm experimental features");

            // Vanilla's own rule, run against what the pack declares. Copied
            // rather than described so this fails if 26.x ever changes how the
            // lifecycle is decided, instead of quietly testing a rule that is
            // no longer the one being applied.
            Lifecycle lifecycle = known.map(KnownPack::isVanilla)
                    .map(vanilla -> Lifecycle.stable())
                    .orElse(Lifecycle.experimental());
            require(lifecycle == Lifecycle.stable(),
                    pack.location().id() + " should yield a stable lifecycle by vanilla's own rule");

            // A mod is not vanilla and must not claim to be: the namespace is
            // what tells a client which pack it already has, and borrowing
            // Minecraft's would make a mod's data indistinguishable from the
            // game's during pack sync.
            require(!known.get().isVanilla(),
                    pack.location().id() + " must not claim minecraft's namespace");
            require(!known.get().version().isEmpty(),
                    pack.location().id() + " should carry a version, or a client cannot tell "
                            + "one build of a mod from another when packs are synced");
        }

        System.out.println("resource conformance: all checks passed");
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("resource conformance failed: " + what);
        }
    }
}
