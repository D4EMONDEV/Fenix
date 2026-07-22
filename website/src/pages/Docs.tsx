import { useEffect, useMemo } from 'react';
import { Link, NavLink, useNavigate, useParams } from 'react-router-dom';
import { findDoc, latestVersion, sidebarFor, versions } from '../lib/content';
import { render } from '../lib/markdown';

export function Docs() {
  const { version = latestVersion, '*': slug = 'index' } = useParams();
  const navigate = useNavigate();

  const doc = findDoc(version, slug);
  const groups = useMemo(() => sidebarFor(version), [version]);
  const rendered = useMemo(() => (doc ? render(doc.body) : null), [doc]);

  useEffect(() => {
    document.title = doc ? `${doc.title} — Fenix` : 'Not found — Fenix';
  }, [doc]);

  // A page linked from an older version may not exist in a newer one. Landing
  // on that version's front page is better than a dead end, and better than
  // refusing to switch.
  function switchVersion(next: string) {
    navigate(findDoc(next, slug) ? `/docs/${next}/${slug}` : `/docs/${next}/index`);
  }

  return (
    <div className="docs shell">
      <aside className="sidebar" aria-label="Documentation">
        <label className="version-picker">
          <span>Version</span>
          <select value={version} onChange={(event) => switchVersion(event.target.value)}>
            {versions.map((candidate) => (
              <option key={candidate} value={candidate}>
                {candidate}
                {candidate === latestVersion ? ' (latest)' : ''}
              </option>
            ))}
          </select>
        </label>

        {groups.map((group) => (
          <nav className="sidebar-group" key={group.id || 'root'}>
            <h2>{group.label}</h2>
            <ul>
              {group.pages.map((page) => (
                <li key={page.slug}>
                  <NavLink to={`/docs/${version}/${page.slug}`} end>
                    {page.title}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        ))}
      </aside>

      {doc && rendered ? (
        <>
          <article className="prose">
            <h1>{doc.title}</h1>
            {doc.description && <p className="page-description">{doc.description}</p>}
            <div dangerouslySetInnerHTML={{ __html: rendered.html }} />
          </article>

          <aside className="toc" aria-label="On this page">
            {rendered.headings.length > 0 && (
              <>
                <h2>On this page</h2>
                <ul>
                  {rendered.headings.map((heading) => (
                    <li key={heading.id} className={`level-${heading.level}`}>
                      <a href={`#${heading.id}`}>{heading.text}</a>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </aside>
        </>
      ) : (
        <article className="prose">
          <h1>Not found</h1>
          <p>
            There is no page at <code>{slug}</code> in version {version}.
          </p>
          <p>
            <Link to={`/docs/${latestVersion}/index`}>Go to the documentation home →</Link>
          </p>
        </article>
      )}
    </div>
  );
}
