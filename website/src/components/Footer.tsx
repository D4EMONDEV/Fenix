import { latestVersion } from '../lib/content';

export function Footer() {
  return (
    <footer className="site-footer">
      <div className="shell">
        <p>
          Fenix is a mod loader for Minecraft 26.2, built by <strong>D4EMON</strong>. Apache-2.0.
        </p>
        <nav aria-label="Footer">
          <a href={`/docs/${latestVersion}/index`}>Documentation</a>
          <a href="https://github.com/D4EMONDEV/Fenix" target="_blank" rel="noreferrer noopener">
            Source
          </a>
          <a
            href="https://github.com/D4EMONDEV/Fenix/releases"
            target="_blank"
            rel="noreferrer noopener"
          >
            Releases
          </a>
          <a href="https://d4emondev.github.io/Fenix/" target="_blank" rel="noreferrer noopener">
            Maven
          </a>
        </nav>
      </div>
    </footer>
  );
}
