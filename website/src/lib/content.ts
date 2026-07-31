/**
 * The documentation, read off disk at build time.
 *
 * Every page is a Markdown file under `content/<line>/…`. A line is mapped to
 * the published Fenix API release it documents, so readers never have to guess
 * whether a number is a website version or an artifact version.
 */

const files = import.meta.glob('../../content/**/*.md', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

export interface Doc {
  /** The published Fenix API version this page documents, e.g. `0.3.0`. */
  version: string;
  /** The path within a version, e.g. `guides/getting-started`. */
  slug: string;
  /** The group a sidebar puts it under, e.g. `guides`; empty for a version's root page. */
  group: string;
  title: string;
  description: string;
  /** Lower sorts first. Pages without one sort last, alphabetically. */
  order: number;
  body: string;
}

/**
 * The source directory is an editorial line. The public route and picker use
 * the API bundle version, which is what a mod author installs.
 *
 * Add a line here only when publishing a new Fenix API release with its own
 * documentation snapshot.
 */
const API_VERSION_FOR_LINE: Record<string, string> = {
  '0.1': '0.3.0',
};

/**
 * Splits the `---` block off the top of a file.
 *
 * Hand-rolled rather than a library: the front matter here is three flat
 * string fields, and every parser that handles more of YAML also drags a
 * Node-shaped dependency into a browser bundle.
 */
function readFrontMatter(raw: string): { data: Record<string, string>; body: string } {
  const match = /^---\r?\n([\s\S]*?)\r?\n---\r?\n?/.exec(raw);
  if (!match) {
    return { data: {}, body: raw };
  }

  const data: Record<string, string> = {};
  for (const line of match[1].split(/\r?\n/)) {
    const field = /^([A-Za-z_]+):\s*(.*)$/.exec(line);
    if (field) {
      data[field[1]] = field[2].trim().replace(/^["']|["']$/g, '');
    }
  }
  return { data, body: raw.slice(match[0].length) };
}

/** A readable title, for a page that did not give itself one. */
function titleFromSlug(slug: string): string {
  const last = slug.split('/').pop() ?? slug;
  return last.replace(/-/g, ' ').replace(/^./, (c) => c.toUpperCase());
}

export const docs: Doc[] = Object.entries(files)
  .map(([path, raw]) => {
    // ../../content/0.1/guides/getting-started.md
    const relative = path.replace(/^.*\/content\//, '').replace(/\.md$/, '');
    const [line, ...rest] = relative.split('/');
    const slug = rest.join('/') || 'index';
    const { data, body } = readFrontMatter(raw);

    return {
      version: API_VERSION_FOR_LINE[line] ?? line,
      slug,
      group: rest.length > 1 ? rest[0] : '',
      title: data.title || titleFromSlug(slug),
      description: data.description || '',
      order: data.order ? Number(data.order) : Number.MAX_SAFE_INTEGER,
      // The page renders its own title from the front matter, so a leading H1
      // in the body would be the same words twice. Pages written for another
      // renderer keep theirs, and this is what lets them be copied unedited.
      body: body
        .replace(/^\s*#\s+.*(\r?\n)+/, '')
        .replaceAll('/docs/0.1/', `/docs/${API_VERSION_FOR_LINE['0.1']}/`),
    };
  })
  .sort((a, b) => a.order - b.order || a.title.localeCompare(b.title));

/** Every version that has documentation, newest first. */
export const versions: string[] = [...new Set(docs.map((doc) => doc.version))].sort((a, b) =>
  b.localeCompare(a, undefined, { numeric: true }),
);

/** The version a bare `/docs` link lands on. */
export const latestVersion = versions[0] ?? '0.3.0';

export function findDoc(version: string, slug: string): Doc | undefined {
  return docs.find((doc) => doc.version === version && doc.slug === slug);
}

export interface SidebarGroup {
  /** The directory name, or empty for the pages sitting at a version's root. */
  id: string;
  label: string;
  pages: Doc[];
}

/** How the groups are named and ordered; anything unlisted follows, as-is. */
const GROUPS: { id: string; label: string }[] = [
  { id: '', label: 'Fenix API' },
  { id: 'guides', label: 'Guides' },
  { id: 'api', label: 'API reference' },
];

export function sidebarFor(version: string): SidebarGroup[] {
  const pages = docs.filter(
    (doc) =>
      doc.version === version &&
      (doc.group === '' || doc.group === 'guides' || doc.group === 'api') &&
      !doc.slug.startsWith('api/fr-d4emon-fenix-loader-'),
  );
  const ids = [...new Set(pages.map((page) => page.group))];

  ids.sort((a, b) => {
    const rank = (id: string) => {
      const at = GROUPS.findIndex((group) => group.id === id);
      return at === -1 ? GROUPS.length : at;
    };
    return rank(a) - rank(b) || a.localeCompare(b);
  });

  return ids.map((id) => ({
    id,
    label: GROUPS.find((group) => group.id === id)?.label ?? titleFromSlug(id),
    pages: pages.filter((page) => page.group === id),
  }));
}
