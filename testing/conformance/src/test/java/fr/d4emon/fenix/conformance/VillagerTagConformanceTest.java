package fr.d4emon.fenix.conformance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a mod shipping a villager profession also ships the tag that makes
 * it reachable.
 *
 * <p>This check exists because the suite missed a real bug. Everything else about
 * the jeweller was right — the point of interest registered, the block-state
 * bookkeeping done, the profession registered and naming its own job site — and
 * in game no villager ever took it. An unemployed villager searches with the
 * {@code none} profession's predicate, and that predicate is not "any registered
 * job site" but "anything in {@code minecraft:acquirable_job_site}". A job site
 * outside that tag is never looked for, and nothing anywhere says so.
 *
 * <p>The registry checks could not have caught it: tags arrive with the
 * datapacks, and a headless probe loads none. So this asks the question of the
 * files instead — the same reasoning as the worldgen check, where what ships is
 * what matters.
 */
class VillagerTagConformanceTest {

    private static final String TAG_FILE =
            "data/minecraft/tags/point_of_interest_type/acquirable_job_site.json";

    @Test
    @DisplayName("the mod's job site is in the tag that lets a villager find it")
    void jobSiteIsAcquirable() throws Exception {
        Path modResources = requiredDir("fenix.test.exampleResources");
        Path tag = modResources.resolve(TAG_FILE);
        assertTrue(Files.isRegularFile(tag),
                "example-mod registers a villager profession, so it has to ship " + TAG_FILE
                        + " — without it the profession is registered and no villager ever takes it");

        JsonElement parsed = JsonParser.parseString(Files.readString(tag, StandardCharsets.UTF_8));
        JsonObject object = parsed.getAsJsonObject();

        // A mod adding to a vanilla tag must not replace it: "replace": true
        // would drop every vanilla job site, and villages would stop working
        // everywhere rather than only here.
        assertTrue(!object.has("replace") || !object.get("replace").getAsBoolean(),
                TAG_FILE + " must not replace the vanilla tag — that would unemploy every "
                        + "vanilla villager in the world");

        List<String> values = new ArrayList<>();
        object.getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
        assertTrue(values.contains("example-mod:jeweller_poi"),
                TAG_FILE + " should name example-mod:jeweller_poi, the jeweller's job site; "
                        + "it names " + values);
    }

    private static Path requiredDir(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "the build must set -D" + property);
        Path path = Path.of(value);
        assertTrue(Files.isDirectory(path), value + " does not exist");
        return path;
    }
}
