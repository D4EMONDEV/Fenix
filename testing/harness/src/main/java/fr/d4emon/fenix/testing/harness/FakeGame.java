package fr.d4emon.fenix.testing.harness;

import fr.d4emon.fenix.loader.launch.FenixHooks;

/**
 * A game in thirty lines: it starts, opens its registries, finishes booting,
 * runs a few ticks, exits.
 *
 * <p>Its only job is to hit the same integration points real Minecraft does, so
 * the loader's whole pipeline can be exercised in a second, headlessly. Where
 * Minecraft gets the {@link FenixHooks} calls injected by the loader's mixins,
 * this game simply makes them — the harness stands in for the mixin layer, not
 * for the game logic.
 *
 * <p>Note the package: deliberately outside the parent-only prefixes, because a
 * real game lives in the child scope and this one must too.
 */
public final class FakeGame {

    private FakeGame() {
    }

    /**
     * Boots the fake game. Run it through the loader:
     * {@code gradlew :test-harness:runDemo}.
     *
     * @param args ignored, as befits a fake game
     */
    public static void main(String[] args) {
        System.out.println("[fake-game] booting (loaded by "
                + FakeGame.class.getClassLoader().getName() + ")");

        // Real Minecraft: BuiltInRegistries.bootStrap(), before the freeze.
        FenixHooks.onGameRegister();

        // Real Minecraft: the tail of the client or server constructor.
        FenixHooks.onGameInit();

        for (int tick = 1; tick <= 3; tick++) {
            System.out.println("[fake-game] tick " + tick);
        }
        System.out.println("[fake-game] closing normally");
    }
}
