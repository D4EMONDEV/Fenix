import { Link } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { latestVersion } from '../lib/content';

/** One of the three things a person actually downloads or runs. */
interface Piece {
  name: string;
  tagline: string;
  body: string;
  points: string[];
  to: string;
  cta: string;
}

const PIECES: Piece[] = [
  {
    name: 'Fenix Loader',
    tagline: 'Starts the game, and starts your mods inside it',
    body: 'Discovery, dependency resolution, class transformation and Mixin — plus the part most loaders leave out: when something is wrong, it says so somewhere you will actually read.',
    points: [
      'Mods found and parsed in parallel, in a stable order',
      'depends, breaks and after — refuse a combination, or just load after one',
      'Two mods carrying the same library keep the newer copy instead of refusing',
      'A failed launch is written beside the game and shown in a window',
    ],
    to: `/docs/${latestVersion}/index`,
    cta: 'Read the documentation',
  },
  {
    name: 'Fenix Installer',
    tagline: 'One double-click, nothing to type',
    body: 'A real application that adds a Fenix profile to the Minecraft Launcher. It carries its own Java, so there is nothing else to install, and it offers the versions it finds rather than asking you to spell one.',
    points: [
      'Windows app image, built with jpackage — no JRE to install',
      'Game versions listed from what the launcher has actually run',
      'Writes a profile that inherits from vanilla; deleting it deletes Fenix',
      'Still a command-line tool when given arguments, for a headless server',
    ],
    to: `/docs/${latestVersion}/play/install`,
    cta: 'Install Fenix',
  },
  {
    name: 'Ember',
    tagline: 'Your assets and data, generated from Java',
    body: 'Models, loot tables, recipes, tags, translations, sounds and ore placement — written as code, generated into your source tree, and reviewable in a diff like everything else.',
    points: [
      'Output lands in src/main/generated and ships in the jar',
      'A block with no loot table drops nothing, silently — Ember writes it',
      'Ore generation: the configured feature and the placed feature, both',
      'Runs with gradlew ember; nothing is generated behind your back',
    ],
    to: `/docs/${latestVersion}/guides/ember`,
    cta: 'Generate resources',
  },
];

/** What is genuinely different, rather than what is merely present. */
const DIFFERENCES: { title: string; body: string }[] = [
  {
    title: 'No remapping, at all',
    body: 'Minecraft has shipped unobfuscated since 26.1. Fenix has no mappings, no refmaps and no remapping step — a mixin targets the game by its real name, and what you read in the decompiler is what you write.',
  },
  {
    title: 'Mods are found at compile time',
    body: 'An annotation processor writes an index into the jar, so the loader never scans classes looking for entry points. A misspelled entry class is a compile error rather than a silent no-op.',
  },
  {
    title: 'The API absorbs vanilla’s bookkeeping',
    body: 'Registering a block means block state ids, the item mapping, creative tabs and more — each one skipped is a crash far from its cause. The registrar does all of it, and a conformance suite proves it against the real game.',
  },
  {
    title: 'Creative tabs that page',
    body: 'Vanilla’s tab strip holds exactly fourteen and vanilla fills all fourteen. Fenix adds pages, with arrows and Page Up/Page Down — and search, inventory and hotbars travel to every page.',
  },
  {
    title: 'Access widening from the manifest',
    body: 'Some of vanilla’s doors are shut in a way no mixin can open. Declare them once; the loader applies them at run time and the Gradle plugin applies them to the jar you compile against, so the two cannot disagree.',
  },
  {
    title: 'Every claim is checked against the game',
    body: 'Thirteen conformance checks boot real Minecraft through the loader, and each one was verified to fail when the thing it covers is sabotaged. A green suite means something here.',
  },
];

export function Home() {
  return (
    <div className="home">
      <section className="hero">
        <div className="shell hero-inner">
          <Logo size={96} />
          <h1>
            A modern mod loader for <span className="grad">Minecraft 26.2</span>
          </h1>
          <p className="lead">
            No mappings. No refmaps. No remapping step. Mods are indexed when they compile, and the
            API takes care of the vanilla bookkeeping that mods otherwise skip and crash on.
          </p>
          <div className="hero-actions">
            <Link className="button primary" to={`/docs/${latestVersion}/play/install`}>
              Install Fenix
            </Link>
            <Link className="button" to={`/docs/${latestVersion}/guides/getting-started`}>
              Write a mod
            </Link>
          </div>
          <p className="hero-note">
            Pre-1.0 and honest about it: it works, and the API still changes without notice.
          </p>
        </div>
      </section>

      <section className="shell band">
        <h2 className="band-title">One line in your build file</h2>
        <div className="snippet">
          <pre>
            <code>{`plugins { id("fr.d4emon.fenix.dev") version "0.1.3" }

fenix { minecraft = "26.2" }`}</code>
          </pre>
        </div>
        <p className="band-note">
          That downloads the game, compiles your mod against it, and gives you{' '}
          <code>runClient</code> and <code>runServer</code>. Nothing else to configure.
        </p>
      </section>

      <section className="shell band">
        <h2 className="band-title">Three pieces</h2>
        <div className="pieces">
          {PIECES.map((piece) => (
            <article className="piece" key={piece.name}>
              <h3>{piece.name}</h3>
              <p className="piece-tagline">{piece.tagline}</p>
              <p>{piece.body}</p>
              <ul>
                {piece.points.map((point) => (
                  <li key={point}>{point}</li>
                ))}
              </ul>
              <Link className="piece-cta" to={piece.to}>
                {piece.cta} →
              </Link>
            </article>
          ))}
        </div>
      </section>

      <section className="shell band">
        <h2 className="band-title">What is different</h2>
        <p className="band-note">
          Fabric, Forge and NeoForge are mature and Fenix is not. These are the places where it does
          something else on purpose, rather than the places where it has caught up.
        </p>
        <div className="differences">
          {DIFFERENCES.map((item) => (
            <article className="difference" key={item.title}>
              <h3>{item.title}</h3>
              <p>{item.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="shell band">
        <h2 className="band-title">Recently landed</h2>
        <ul className="changes">
          <li>
            <strong>World generation</strong> — ore placement written from Java, and biome
            modifications that compose instead of overwriting each other.
          </li>
          <li>
            <strong>Key bindings</strong> — including the part vanilla makes hard: a key that
            survives a restart.
          </li>
          <li>
            <strong>Spawn eggs and spawn rules</strong> — an entity that appears on its own, not
            only by command.
          </li>
          <li>
            <strong>Menus and access widening</strong> — <code>SimpleMenu</code> lays out slots and
            gets <code>quickMoveStack</code> right.
          </li>
          <li>
            <strong>Kotlin entry points</strong> — an <code>object</code> is used as it is, rather
            than refused for having no constructor.
          </li>
        </ul>
        <p className="band-note">
          The full list is in the{' '}
          <a
            href="https://github.com/D4EMONDEV/Fenix/blob/main/CHANGELOG.md"
            target="_blank"
            rel="noreferrer noopener"
          >
            changelog
          </a>
          .
        </p>
      </section>
    </div>
  );
}
