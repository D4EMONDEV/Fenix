import { Link } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { latestVersion } from '../lib/content';
import { FENIX_VERSION, MINECRAFT_VERSION } from '../lib/template';

export function Home() {
  return (
    <div className="home-brief shell">
      <section className="brief-intro">
        <Logo size={72} />
        <div>
          <p className="eyebrow">Minecraft {MINECRAFT_VERSION} · Java 25</p>
          <h1>Fenix</h1>
          <p>
            A focused API and resource-generation toolkit for modern Minecraft modding. Built for
            readable Java, explicit lifecycle phases and generated game data.
          </p>
        </div>
      </section>

      <section className="brief-links" aria-label="Explore Fenix">
        <Link to={`/docs/${latestVersion}/index`}>
          <span>01</span>
          <div><strong>Fenix API {FENIX_VERSION}</strong><p>Guides, concepts and generated API reference.</p></div>
          <b>→</b>
        </Link>
        <Link to={`/docs/${latestVersion}/guides/ember`}>
          <span>02</span>
          <div><strong>Ember</strong><p>Generate models, language files, recipes and data from Java.</p></div>
          <b>→</b>
        </Link>
        <Link to="/generate">
          <span>03</span>
          <div><strong>Project generator</strong><p>Create a minimal mod project, with Ember only when you select it.</p></div>
          <b>→</b>
        </Link>
      </section>

      <section className="brief-note">
        <p><strong>Version policy.</strong> Fenix begins at Minecraft 26.2 and will never support an older release. Each Fenix release targets one exact Minecraft version.</p>
        <a href="https://github.com/D4EMONDEV/Fenix" target="_blank" rel="noreferrer noopener">View source on GitHub ↗</a>
      </section>
    </div>
  );
}
