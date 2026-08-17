#!/usr/bin/env bash
# Builds a mod from nothing, resolving only from the published Maven repository.
#
# Everything else in this repository proves Fenix works when Fenix is already
# there: the demo is built beside the modules it uses, and the conformance suite
# runs against the local build. Neither says whether someone who has never
# cloned this repository can write a mod.
#
# That is what this checks, and it has already found what nothing else would:
# the site's own "Your first mod" page shipped a build file that could not
# build. The plugin is not on the Gradle Plugin Portal, so the page needed a
# settings file it did not have, and it declared a `fenixApi` dependency that
# is not a configuration the plugin creates. The version check passed the whole
# time -- it read the version out of a line that never worked.
#
#     tools/verify-fresh-mod.sh
#
# Pass a directory to keep the project around afterwards for poking at.
set -euo pipefail

REPO="https://d4emondev.github.io/Fenix/"
HERE="$(cd "$(dirname "$0")/.." && pwd)"
DIR="${1:-$(mktemp -d)}"
rm -rf "$DIR"; mkdir -p "$DIR/src/main/java/com/example/mymod" "$DIR/src/main/resources"

# The versions a reader is handed, taken from the table rather than typed here:
# this is meant to fail when what is published and what is documented disagree.
read_platform() {
    python -c "import json,sys;d=json.load(sys.stdin);p=d['platforms'][0];print($1)"         < "$HERE/platforms.json"
}
API="$(read_platform "p['api']+'+mc'+p['minecraft']")"
PLUGIN="$(read_platform "d['plugin']")"
MC="$(read_platform "p['minecraft']")"
echo "verifying against plugin $PLUGIN, api $API, Minecraft $MC"

cat > "$DIR/settings.gradle" <<EOF
pluginManagement {
    repositories {
        maven { url = '$REPO' }
        gradlePluginPortal()
    }
}

rootProject.name = 'mymod'
EOF

# No dependencies block: the plugin supplies the API. If that ever stops being
# true, this build fails on a missing symbol rather than in someone's editor.
cat > "$DIR/build.gradle" <<EOF
plugins {
    id 'java'
    id 'fr.d4emon.fenix.dev' version '$PLUGIN'
}

fenix {
    minecraft = '$MC'
}
EOF

cat > "$DIR/src/main/resources/fenix.mod.json" <<'EOF'
{
  "schema": 1,
  "id": "mymod",
  "version": "1.0.0",
  "depends": { "fenix": ">=0.1.0", "fenix-api-registry": ">=0.4.0" }
}
EOF

cat > "$DIR/src/main/java/com/example/mymod/MyMod.java" <<'EOF'
package com.example.mymod;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.registry.Holder;
import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.world.level.block.Block;

@Mod("mymod")
public final class MyMod implements FenixMod {

    public static final Registrar REGISTRAR = Registrar.of("mymod");

    public static final Holder<Block> RUBY_BLOCK = REGISTRAR.newBlock("ruby_block")
            .strength(3f, 6f)
            .requiresTool()
            .withItem()
            .register();

    public MyMod() {
    }

    @Override
    public void onRegister(Fenix fenix) {
        REGISTRAR.apply();
    }

    @Override
    public void onInit(Fenix fenix) {
        fenix.logger().info("mymod is up");
    }
}
EOF

# --refresh-dependencies so a copy cached from a previous run cannot stand in
# for one that was never actually published.
"$HERE/gradlew" -p "$DIR" build --refresh-dependencies --no-build-cache -q

test -f "$DIR/build/libs/mymod.jar" || { echo "no jar was produced"; exit 1; }
unzip -l "$DIR/build/libs/mymod.jar" | grep -q "fenix.index.json" \
    || { echo "the jar has no fenix.index.json, so the annotation processor did not run"; exit 1; }

# And that the loader finds it. A dry run resolves everything and stops before
# the game window, which is the last thing that can be checked without a screen.
"$HERE/gradlew" -p "$DIR" runClient -Pfenix.dryRun -q 2>&1 | tee "$DIR/dry-run.log" | tail -3
grep -q "loading mymod" "$DIR/dry-run.log" || { echo "the loader did not load the mod"; exit 1; }
grep -q "fenix-api $API" "$DIR/dry-run.log" \
    || { echo "the loader did not resolve fenix-api $API"; exit 1; }

echo "a mod built from nothing, against the published repository alone"
