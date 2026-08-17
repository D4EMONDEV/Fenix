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

    @Test
    @DisplayName("every texture an equipment asset names is a file that exists")
    void equipmentTexturesExist() throws IOException {
        List<Path> trees = trees();
        List<String> problems = new ArrayList<>();
        int checked = 0;

        for (Path tree : trees) {
            Path assets = tree.resolve("assets/example-mod/equipment");
            if (!Files.isDirectory(assets)) {
                continue;
            }
            try (Stream<Path> files = Files.list(assets)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    JsonObject layers = JsonParser
                            .parseString(Files.readString(file, StandardCharsets.UTF_8))
                            .getAsJsonObject().getAsJsonObject("layers");
                    if (layers == null) {
                        problems.add(file.getFileName() + " has no layers, so nothing wearing "
                                + "it is drawn");
                        continue;
                    }

                    for (Map.Entry<String, JsonElement> layer : layers.entrySet()) {
                        for (JsonElement entry : layer.getValue().getAsJsonArray()) {
                            String texture = entry.getAsJsonObject().get("texture").getAsString();
                            if (!texture.startsWith("example-mod:")) {
                                continue;
                            }
                            checked++;
                            // The layer's name is the directory's name, one for
                            // one — humanoid_leggings is a separate file from
                            // humanoid because it is a separate model layer.
                            String path = "assets/example-mod/textures/entity/equipment/"
                                    + layer.getKey() + "/"
                                    + texture.substring("example-mod:".length()) + ".png";
                            if (!exists(trees, path)) {
                                problems.add(file.getFileName() + ": the " + layer.getKey()
                                        + " layer names " + texture + ", and "
                                        + path + " does not exist");
                            }
                        }
                    }
                }
            }
        }

        assertTrue(checked > 0,
                "no equipment layers found; either the demo stopped shipping armour or this "
                        + "check has stopped reading it");
        assertTrue(problems.isEmpty(),
                "armour that equips, protects, wears down and cannot be seen:\n  "
                        + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("every sound sounds.json names is an ogg that exists")
    void soundFilesExist() throws IOException {
        List<Path> trees = trees();
        List<String> problems = new ArrayList<>();
        int checked = 0;

        for (Path tree : trees) {
            Path definitions = tree.resolve("assets/example-mod/sounds.json");
            if (!Files.isRegularFile(definitions)) {
                continue;
            }
            JsonObject events = JsonParser
                    .parseString(Files.readString(definitions, StandardCharsets.UTF_8))
                    .getAsJsonObject();

            for (Map.Entry<String, JsonElement> event : events.entrySet()) {
                JsonElement sounds = event.getValue().getAsJsonObject().get("sounds");
                if (sounds == null) {
                    problems.add(event.getKey() + " names no sounds, so it plays nothing");
                    continue;
                }
                for (JsonElement entry : sounds.getAsJsonArray()) {
                    // An entry is either a name or an object carrying one.
                    String name = entry.isJsonPrimitive()
                            ? entry.getAsString()
                            : entry.getAsJsonObject().get("name").getAsString();
                    if (!name.startsWith("example-mod:")) {
                        continue;
                    }
                    checked++;
                    String path = "assets/example-mod/sounds/"
                            + name.substring("example-mod:".length()) + ".ogg";
                    Path file = fileIn(trees, path);
                    if (file == null) {
                        problems.add(event.getKey() + " plays " + name + ", and "
                                + path + " does not exist");
                        continue;
                    }
                    // Ogg and nothing else. The game reads Ogg Vorbis, and a
                    // wav or mp3 renamed to .ogg is a file that is there,
                    // resolves, and plays silence — which is the same symptom
                    // as no file at all, from one directory further away.
                    byte[] head = Files.readAllBytes(file);
                    if (head.length < 4 || head[0] != 'O' || head[1] != 'g'
                            || head[2] != 'g' || head[3] != 'S') {
                        problems.add(path + " is not an Ogg stream; it starts with "
                                + new String(head, 0, Math.min(4, head.length),
                                        StandardCharsets.ISO_8859_1));
                    }
                }
            }
        }

        assertTrue(checked > 0,
                "no sounds found; either the demo stopped shipping any or this check has "
                        + "stopped reading them");
        assertTrue(problems.isEmpty(),
                "sounds that register, resolve and play nothing — the log says nothing "
                        + "either:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("every trim and animal variant names a texture that exists")
    void trimAndVariantTexturesExist() throws IOException {
        List<Path> trees = trees();
        List<String> problems = new ArrayList<>();
        int checked = 0;

        for (Path tree : trees) {
            Path data = tree.resolve("data/example-mod");
            if (!Files.isDirectory(data)) {
                continue;
            }

            // A trim pattern's asset is drawn on two layers, and each layer is
            // a directory. A pattern with only the first is armour that is
            // trimmed on the body and plain on the legs.
            Path patterns = data.resolve("trim_pattern");
            if (Files.isDirectory(patterns)) {
                try (Stream<Path> files = Files.list(patterns)) {
                    for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                        String asset = JsonParser
                                .parseString(Files.readString(file, StandardCharsets.UTF_8))
                                .getAsJsonObject().get("asset_id").getAsString();
                        if (!asset.startsWith("example-mod:")) {
                            continue;
                        }
                        String local = asset.substring("example-mod:".length());
                        for (String layer : List.of("humanoid", "humanoid_leggings")) {
                            checked++;
                            String path = "assets/example-mod/textures/trims/entity/"
                                    + layer + "/" + local + ".png";
                            if (!exists(trees, path)) {
                                problems.add(file.getFileName() + " names " + asset
                                        + ", and " + path + " does not exist — a trim whose "
                                        + "texture is missing is not drawn at all, so the "
                                        + "armour looks untrimmed rather than broken");
                            }
                        }
                    }
                }
            }

            // An animal variant names its texture and its baby's, and the two
            // are separate files.
            try (Stream<Path> dirs = Files.list(data)) {
                for (Path dir : dirs.filter(Files::isDirectory)
                        .filter(d -> d.getFileName().toString().endsWith("_variant")).toList()) {
                    try (Stream<Path> files = Files.list(dir)) {
                        for (Path file : files.filter(f -> f.toString().endsWith(".json"))
                                .toList()) {
                            JsonObject variant = JsonParser
                                    .parseString(Files.readString(file, StandardCharsets.UTF_8))
                                    .getAsJsonObject();
                            for (String key : List.of("asset_id", "baby_asset_id")) {
                                JsonElement value = variant.get(key);
                                if (value == null || !value.getAsString()
                                        .startsWith("example-mod:")) {
                                    continue;
                                }
                                checked++;
                                // A painting's asset lives under textures/painting/
                                // rather than at the root, so the directory it
                                // came from decides where to look. Both are
                                // "*_variant" directories and neither reports a
                                // missing texture, so both are checked here.
                                String prefix = dir.getFileName().toString()
                                        .equals("painting_variant") ? "painting/" : "";
                                String path = "assets/example-mod/textures/" + prefix
                                        + value.getAsString().substring("example-mod:".length())
                                        + ".png";
                                if (!exists(trees, path)) {
                                    problems.add(file.getFileName() + " (" + key + ") names "
                                            + value.getAsString() + ", and " + path
                                            + " does not exist");
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue(checked > 0,
                "no trims or variants found; either the demo stopped shipping them or this "
                        + "check has stopped reading them");
        assertTrue(problems.isEmpty(),
                "cosmetics that load and cannot be seen:\n  " + String.join("\n  ", problems));
    }

    /** The generated tree and the hand-written one, in that order. */
    private static List<Path> trees() {
        return List.of(requiredDir("fenix.test.exampleGenerated"),
                requiredDir("fenix.test.exampleResources"));
    }

    /** {@return the first tree holding this file, or null} */
    private static Path fileIn(List<Path> trees, String relative) {
        return trees.stream().map(tree -> tree.resolve(relative))
                .filter(Files::isRegularFile).findFirst().orElse(null);
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
