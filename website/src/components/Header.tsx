import { NavLink, Link } from 'react-router-dom';
import { Logo } from './Logo';
import { ThemeToggle } from './ThemeToggle';
import { latestVersion } from '../lib/content';

export function Header() {
  return (
    <header className="site-header">
      <div className="shell">
        <Link className="brand" to="/">
          <Logo size={28} />
          <span className="wordmark">Fenix</span>
        </Link>

        <nav className="site-nav" aria-label="Main">
          <NavLink to={`/docs/${latestVersion}/index`}>API Documentation</NavLink>
          <NavLink to={`/docs/${latestVersion}/guides/ember`}>Ember</NavLink>
          <a href="https://github.com/D4EMONDEV/Fenix" target="_blank" rel="noreferrer noopener">
            GitHub
          </a>
        </nav>

        <NavLink className="header-cta" to="/generate">New project <span>→</span></NavLink>
        <ThemeToggle />
      </div>
    </header>
  );
}
