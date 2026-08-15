package fr.d4emon.fenix.conformance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a mod's assets refer to things that are there.
 *
 * <p>Every failure here draws as the magenta-and-black checker, which is the
 * game saying "something is missing" and nothing more. It does not say whether
 * the texture, the model or the definition that picks the model is the part
 * that is absent, and the three are separate files in three directories. Three
 * causes, one symptom, no log line.
 *
 * <p>All three have now happened in this demo at once: a door whose textures
 * were described in a comment and never drawn, a bucket with a model and no
 * definition beside it, and a spawn egg with no name. Each was reported from
 * the game, by eye, several sessions after it was introduced.
 *
 * <p>Both trees are searched, because a mod writes assets by hand and by
 * generator and the game does not care which produced a file.
 */
class AssetConformanceTest {

    @Test
    @DisplayName("every texture a model names is a file that exists")
    void modelsNameRealTextures() throws IOException {
        List<Path> trees = trees();
        Set<String> problems = new LinkedHashSet<>();

        for (Path tree : trees) {
            Path models = tree.resolve("assets/example-mod/models");
            if (!Files.isDirectory(models)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(models)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    JsonElement parsed = JsonParser.parseString(
                            Files.readString(file, StandardCharsets.UTF_8));
                    JsonObject textures = parsed.getAsJsonObject().getAsJsonObject("textures");
                    if (textures == null) {
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                        String texture = entry.getValue().getAsString();
                        // Only the mod's own. A model borrowing a vanilla
                        // texture is ordinary and the jar is not searched here.
                        if (!texture.startsWith("example-mod:")) {
                            continue;
                        }
                        String path = texture.substring("example-mod:".length()) + ".png";
                        if (!exists(trees, "assets/example-mod/textures/" + path)) {
                            problems.add(texture + " (" + entry.getKey() + " of "
                                    + file.getFileName() + ") has no file");
                        }
                    }
                }
            }
        }

        assertTrue(problems.isEmpty(),
                "models naming textures that do not exist, which draw as the missing "
                        + "checker:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("every item has a definition choosing its model, and the model exists")
    void itemsHaveDefinitions() throws IOException {
        List<Path> trees = trees();
        List<String> problems = new ArrayList<>();

        for (Path tree : trees) {
            Path models = tree.resolve("assets/example-mod/models/item");
            if (!Files.isDirectory(models)) {
                continue;
            }
            try (Stream<Path> files = Files.list(models)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    String name = file.getFileName().toString();
                    // In 26.2 the model file is not what the game looks up: an
                    // item is drawn through its definition in items/, and an
                    // item without one is the missing texture however good its
                    // model is. That is exactly how the brine bucket failed.
                    if (!exists(trees, "assets/example-mod/items/" + name)) {
                        problems.add(name.replace(".json", "")
                                + " has a model but no definition in items/");
                    }
                }
            }
        }

        assertTrue(problems.isEmpty(),
                "items that cannot be drawn:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("every item that can be drawn can also be named")
    void itemsHaveNames() throws IOException {
        List<Path> trees = trees();
        JsonObject english = null;
        for (Path tree : trees) {
            Path lang = tree.resolve("assets/example-mod/lang/en_us.json");
            if (Files.isRegularFile(lang)) {
                english = JsonParser.parseString(Files.readString(lang, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            }
        }
        assertNotNull(english, "en_us is the language every key is defined in");

        List<String> problems = new ArrayList<>();
        for (Path tree : trees) {
            Path items = tree.resolve("assets/example-mod/items");
            if (!Files.isDirectory(items)) {
                continue;
            }
            try (Stream<Path> files = Files.list(items)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    String id = file.getFileName().toString().replace(".json", "");
                    // Either key will do: a block's item is named by the block.
                    if (!english.has("item.example-mod." + id)
                            && !english.has("block.example-mod." + id)) {
                        problems.add(id);
                    }
                }
            }
        }

        assertTrue(problems.isEmpty(),
                "items with no name, which show their translation key instead: "
                        + String.join(", ", problems));
    }

    @Test
    @DisplayName("a door's texture is opaque where its narrow faces sample it")
    void doorEdgesAreOpaque() throws IOException {
        List<Path> trees = trees();
        List<String> problems = new ArrayList<>();

        for (Path tree : trees) {
            Path models = tree.resolve("assets/example-mod/models/block");
            if (!Files.isDirectory(models)) {
                continue;
            }
            try (Stream<Path> files = Files.list(models)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    JsonObject model = JsonParser.parseString(
                            Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    JsonElement parent = model.get("parent");
                    if (parent == null
                            || !parent.getAsString().startsWith("minecraft:block/door_")) {
                        continue;
                    }
                    JsonObject textures = model.getAsJsonObject("textures");
                    if (textures == null) {
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                        String texture = entry.getValue().getAsString();
                        if (!texture.startsWith("example-mod:")) {
                            continue;
                        }
                        String path = "assets/example-mod/textures/"
                                + texture.substring("example-mod:".length()) + ".png";
                        for (Path tex : trees) {
                            Path png = tex.resolve(path);
                            if (Files.isRegularFile(png)) {
                                problems.addAll(clearPixels(png, texture));
                                break;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(problems.isEmpty(),
                "a door is three pixels thick, and vanilla's door models take those edges "
                        + "from uv [0,0,3,16] and the bottom three rows of the same texture. "
                        + "A clear pixel there is a see-through edge on a solid door:\n  "
                        + String.join("\n  ", problems));
    }

    /**
     * Fully transparent pixels in the strips a door model samples for its
     * edges: the three leftmost columns, and the bottom three rows.
     */
    private static List<String> clearPixels(Path png, String name) throws IOException {
        BufferedImage image = ImageIO.read(png.toFile());
        assertNotNull(image, name + " is not an image this JDK can read");

        // Scaled to the image, so a 32x32 or 64x64 texture is measured in the
        // same sixteenths the model's uv coordinates are written in.
        int unit = image.getWidth() / 16;
        int edge = Math.max(1, 3 * unit);

        List<String> problems = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                boolean sampled = x < edge || y >= image.getHeight() - edge;
                if (sampled && (image.getRGB(x, y) >>> 24) == 0) {
                    problems.add(name + " is clear at (" + x + ", " + y + ")");
                    // One is enough to make the point, and a blank strip would
                    // otherwise report every pixel in it.
                    return problems;
                }
            }
        }
        return problems;
    }

    /** The generated tree and the hand-written one, in that order. */
    private static List<Path> trees() {
        return List.of(requiredDir("fenix.test.exampleGenerated"),
                requiredDir("fenix.test.exampleResources"));
    }

    private static boolean exists(List<Path> trees, String relative) {
        return trees.stream().anyMatch(tree -> Files.isRegularFile(tree.resolve(relative)));
    }

    private static Path requiredDir(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "the build must set -D" + property);
        Path path = Path.of(value);
        assertTrue(Files.isDirectory(path), value + " does not exist");
        return path;
    }
}
