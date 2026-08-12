import { Link } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { latestVersion } from '../lib/content';
import { currentPlatform, pluginVersion } from '../lib/platforms';

/** Where a reader can go next, in the order most of them want. */
const PATHS: { title: string; body: string; to: string }[] = [
  {
    title: 'Install Fenix',
    body: 'An installer that carries its own Java and adds a profile to the Minecraft Launcher.',
    to: `/docs/${latestVersion}/play/install`,
  },
  {
    title: 'Start a mod',
    body: 'A name, a package, and a project that builds and runs as it comes.',
    to: '/generate',
  },
  {
    title: 'Read the guides',
    body: 'Content, events, networking, data generation and mixins. Written by hand.',
    to: `/docs/${latestVersion}/index`,
  },
  {
    title: 'Compare it',
    body: 'Where Fenix does something else on purpose, and where it simply has less.',
    to: `/docs/${latestVersion}/why/comparison`,
  },
];

/** What is genuinely different, rather than what is merely present. */
const NOTES: { title: string; body: string }[] = [
  {
    title: 'No remapping, at all',
    body: 'Minecraft has shipped unobfuscated since 26.1. No mappings, no refmaps, no remapping step — a mixin targets the game by its real name.',
  },
  {
    title: 'Mods are found at compile time',
    body: 'An annotation processor writes an index into the jar. A misspelled entry class is a compile error rather than a silent no-op.',
  },
  {
    title: 'The client cannot be reached by accident',
    body: 'Common code compiles against a Minecraft with the client removed, so touching a renderer there fails at your desk, not on somebody else’s server.',
  },
  {
    title: 'One version to name',
    body: 'A build names the Minecraft version. Loader, API and Ember are looked up for it, so a mod cannot compile against an API built for another game.',
  },
];

const BUILD_FILE = `plugins {
    id("fr.d4emon.fenix.dev") version "${pluginVersion}"
}

fenix {
    minecraft = "${currentPlatform.minecraft}"
}`;

export function Home() {
  return (
    <div className="home">
      <section className="hero">
        <div className="shell hero-grid">
          <div>
            <p className="kicker">Minecraft {currentPlatform.minecraft} · Java {currentPlatform.java}</p>
            <h1>
              A mod loader for a game that no longer needs <em>deobfuscating</em>.
            </h1>
            <p className="lead">
              Fenix loads mods and gives them an API to write against. The parts other
              loaders leave silent — a block missing from a table, a job site outside a
              tag — are the parts it says out loud.
            </p>
            <p className="actions">
              <Link className="button primary" to={`/docs/${latestVersion}/play/install`}>
                Install Fenix
              </Link>
              <Link className="button" to="/generate">
                Start a mod
              </Link>
            </p>
          </div>

          <aside className="mark-panel" aria-hidden="true">
            <Logo size={132} />
            <dl className="mark-versions">
              <div>
                <dt>loader</dt>
                <dd>{currentPlatform.loader}</dd>
              </div>
              <div>
                <dt>api</dt>
                <dd>{currentPlatform.api}</dd>
              </div>
              <div>
                <dt>ember</dt>
                <dd>{currentPlatform.ember}</dd>
              </div>
              <div>
                <dt>plugin</dt>
                <dd>{pluginVersion}</dd>
              </div>
            </dl>
          </aside>
        </div>
      </section>

      <section className="shell quickstart">
        <div>
          <h2>One line in your build file</h2>
          <p>
            It downloads the game, compiles your mod against it, and adds{' '}
            <code>runClient</code> and <code>runServer</code>. There is no mappings
            block, because there are no mappings.
          </p>
          <Link className="more" to={`/docs/${latestVersion}/guides/getting-started`}>
            Getting started
          </Link>
        </div>

        <figure className="code">
          <span className="lang">build.gradle.kts</span>
          <pre>
            <code>{BUILD_FILE}</code>
          </pre>
        </figure>
      </section>

      <section className="paths-section">
        <div className="shell">
          <h2>Where to go</h2>
          <div className="paths">
            {PATHS.map((path) => (
              <Link className="path" to={path.to} key={path.title}>
                <strong>{path.title}</strong>
                <span>{path.body}</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="shell notes">
        <h2>What is different</h2>
        <ol>
          {NOTES.map((note) => (
            <li key={note.title}>
              <h3>{note.title}</h3>
              <p>{note.body}</p>
            </li>
          ))}
        </ol>
        <p className="note">
          Every version above is read from{' '}
          <a
            href="https://github.com/D4EMONDEV/Fenix/blob/main/platforms.json"
            target="_blank"
            rel="noreferrer noopener"
          >
            platforms.json
          </a>
          , the same file the Gradle plugin carries.
        </p>
      </section>
    </div>
  );
}
