/**
 * Which Fenix versions go with which Minecraft version.
 *
 * Read from the repository's own `platforms.json` — the same file the Gradle
 * plugin carries and reads at build time. The site could keep its own copy of
 * these numbers, and did; every release then had two places to edit and one of
 * them was the one nobody remembered. A generated project that names a version
 * nobody published is a project that fails on its first build, and the visitor
 * has no way to tell it was the website that was wrong.
 *
 * @see fr.d4emon.fenix.gradle.Platforms
 */
import table from '../../../platforms.json';

export interface Platform {
  minecraft: string;
  branch: string;
  /** `current` for the line being developed, `maintenance` for an older one. */
  status: string;
  java: number;
  loader: string;
  api: string;
  ember: string;
  processor: string;
}

export const platforms: Platform[] = table.platforms;

/** The one a visitor gets unless they pick another: the current line. */
export const currentPlatform: Platform = platforms[0];

/**
 * The Gradle plugin version a mod's build file applies.
 *
 * Not a per-platform field: one plugin release carries the whole table and
 * builds for every game version in it.
 */
export const pluginVersion: string = table.plugin;
