import { Link } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { latestVersion } from '../lib/content';
import { FENIX_VERSION, MINECRAFT_VERSION } from '../lib/template';

/** One of the four things a reader can go and do. */
interface Route {
  kicker: string;
  icon: string;
  title: string;
  body: string;
  action: string;
  to: string;
  external?: boolean;
  /** Rendered on paper rather than charcoal, to break the grid up. */
  paper?: boolean;
}

const ROUTES: Route[] = [
  {
    kicker: 'Play',
    icon: '▸',
    title: 'Install Fenix',
    body: 'An installer that carries its own Java and adds a profile to the Minecraft Launcher. Nothing else to install, nothing to type.',
    action: 'Installation guide',
    to: `/docs/${latestVersion}/play/install`,
  },
  {
    kicker: 'Build',
    icon: '{ }',
    title: 'Start a mod',
    body: 'Fill in a name and a package, and download a project that builds — sources, manifest, build files and the Gradle wrapper.',
    action: 'Open the generator',
    to: '/generate',
    paper: true,
  },
  {
    kicker: 'Learn',
    icon: '¶',
    title: `Fenix API ${latestVersion}`,
    body: 'Guides for content, world generation, events and networking, plus a reference generated from the compiler rather than written by hand.',
    action: 'Read the documentation',
    to: `/docs/${latestVersion}/index`,
  },
  {
    kicker: 'Generate',
    icon: '✦',
    title: 'Ember',
    body: 'Models, loot tables, recipes, tags and translations written as Java, generated into your source tree and reviewable in a diff.',
    action: 'Generate resources',
    to: `/docs/${latestVersion}/guides/ember`,
  },
];

/** What is genuinely different, rather than what is merely present. */
const SIGNALS: { title: string; body: string }[] = [
  {
    title: 'No remapping, at all',
    body: 'Minecraft has shipped unobfuscated since 26.1. No mappings, no refmaps, no remapping step — a mixin targets the game by its real name.',
  },
  {
    title: 'Mods are found at compile time',
    body: 'An annotation processor writes an index into the jar. A misspelled entry class is a compile error rather than a silent no-op.',
  },
  {
    title: 'The API absorbs the bookkeeping',
    body: 'Registering a block means block state ids, the item mapping, creative tabs. Each one skipped is a crash far from its cause.',
  },
];

export function Home() {
  return (
    <div className="home">
      <section className="hero">
        <div className="shell hero-grid">
          <div className="hero-copy">
            <p className="eyebrow">
              <span />
              Minecraft {MINECRAFT_VERSION} · Java 25
            </p>
            <h1>
              A mod loader that <em>tells you</em> what is wrong
            </h1>
            <p className="lead">
              Fenix is a Minecraft mod loader and API built for a game that no longer needs
              deobfuscating. The parts other loaders leave silent — a block missing from a
              table, a job site outside a tag — are the parts it says out loud.
            </p>
            <div className="hero-actions">
              <Link className="button primary" to={`/docs/${latestVersion}/play/install`}>
                Install Fenix
              </Link>
              <Link className="button ghost" to="/generate">
                Start a mod
              </Link>
            </div>
            <div className="hero-meta">
              <span>
                <i />
                API {latestVersion} · loader 0.1.1
              </span>
              <span>Gradle plugin {FENIX_VERSION}</span>
              <span>19 conformance checks</span>
            </div>
          </div>

          <div className="hero-mark" aria-hidden="true">
            <div className="mark-orbit orbit-one" />
            <div className="mark-orbit orbit-two" />
            <div className="mark-core">
              <Logo size={150} />
            </div>
            <p>fenix</p>
            <small>MOD LOADER</small>
          </div>
        </div>
      </section>

      <section className="shell route-section">
        <div className="section-intro">
          <h2>Four ways in</h2>
          <p className="lead">
            Whether you want to play with mods, write one, or read how it works.
          </p>
        </div>
        <div className="route-grid">
          {ROUTES.map((route) => (
            <article className={`route-card${route.paper ? ' dev-card' : ''}`} key={route.title}>
              <div className="route-icon">{route.icon}</div>
              <p className="route-kicker">{route.kicker}</p>
              <h3>{route.title}</h3>
              <p>{route.body}</p>
              <Link to={route.to}>
                {route.action} <span>→</span>
              </Link>
            </article>
          ))}
        </div>
      </section>

      <section className="signal-section">
        <div className="shell">
          <p className="eyebrow">What is different</p>
          <div className="signal-heading">
            <h2>Deliberate, not merely newer</h2>
            <Link to={`/docs/${latestVersion}/why/comparison`}>
              Compared with Fabric, Forge and NeoForge <span>→</span>
            </Link>
          </div>
          <div className="signal-grid">
            {SIGNALS.map((signal, index) => (
              <article key={signal.title}>
                <span className="feature-number">0{index + 1}</span>
                <h3>{signal.title}</h3>
                <p>{signal.body}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="shell launch-section">
        <div>
          <p className="eyebrow">Getting started</p>
          <h2>One line in your build file</h2>
          <p>
            That downloads the game, compiles your mod against it, and gives you{' '}
            <code>runClient</code> and <code>runServer</code>. There is no mappings block, because
            there are no mappings.
          </p>
          <Link className="button" to={`/docs/${latestVersion}/guides/getting-started`}>
            Getting started
          </Link>
        </div>

        <div className="launch-panel">
          <span className="terminal-dot red" />
          <span className="terminal-dot amber" />
          <span className="terminal-dot green" />
          <pre>
            <code>
              <span>{'// build.gradle.kts'}</span>
              {'\n'}
              plugins {'{'} id(<b>"fr.d4emon.fenix.dev"</b>) version <b>"{FENIX_VERSION}"</b> {'}'}
              {'\n\n'}
              fenix {'{'} minecraft = <b>"{MINECRAFT_VERSION}"</b> {'}'}
              {'\n\n'}
              <span>$</span> ./gradlew runClient{'\n'}
              <i>&gt; Fenix Loader 0.1.1 — client side</i>
            </code>
          </pre>
        </div>
      </section>
    </div>
  );
}
