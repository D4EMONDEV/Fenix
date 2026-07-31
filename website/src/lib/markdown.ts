import { Marked } from 'marked';
import { latestVersion } from './content';
import hljs from 'highlight.js/lib/core';
import java from 'highlight.js/lib/languages/java';
import json from 'highlight.js/lib/languages/json';
import bash from 'highlight.js/lib/languages/bash';
import kotlin from 'highlight.js/lib/languages/kotlin';
import xml from 'highlight.js/lib/languages/xml';

// Registered one by one rather than pulling in the full language pack: the
// whole of highlight.js is larger than everything else on this site put
// together, and Fenix documentation is written in five languages.
hljs.registerLanguage('java', java);
hljs.registerLanguage('json', json);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('xml', xml);

/** A heading, for the table of contents down the right-hand side. */
export interface Heading {
  id: string;
  text: string;
  level: 2 | 3;
}

/** Turns a heading into something usable in a URL, and stable across builds. */
export function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/`/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

/**
 * Turns `:::note[Title] … :::` blocks into asides.
 *
 * The syntax comes from the documentation these pages were written in, and it
 * earns its keep: an aside is what a caveat should look like, and the
 * alternative is a paragraph that reads exactly like the ones around it.
 */
function admonitions(markdown: string): string {
  return markdown.replace(
    /^:::(note|tip|caution|danger)(?:\[([^\]]*)\])?[^\S\n]*\n([\s\S]*?)^:::[^\S\n]*$/gm,
    (_all, kind: string, title: string | undefined, body: string) => {
      const heading = title ?? kind[0].toUpperCase() + kind.slice(1);
      // Blank lines around the body so Markdown still parses it as Markdown,
      // rather than as one paragraph that happens to sit inside a tag.
      return [
        `<aside class="admonition ${kind}">`,
        `<p class="admonition-title">${heading}</p>`,
        '',
        body.trimEnd(),
        '',
        '</aside>',
      ].join('\n');
    },
  );
}

/**
 * Renders a page, and collects its headings on the way through.
 *
 * One pass for both, because the anchors the table of contents links to are
 * the ones the renderer writes — deriving them separately is how a contents
 * list ends up pointing at anchors that do not exist.
 */
export function render(markdown: string): { html: string; headings: Heading[] } {
  markdown = admonitions(markdown);
  const headings: Heading[] = [];
  const seen = new Map<string, number>();

  const marked = new Marked({
    gfm: true,
    breaks: false,
    renderer: {
      heading({ tokens, depth }) {
        const text = this.parser.parseInline(tokens);
        const plain = text.replace(/<[^>]+>/g, '');

        // Two methods on a class can share a name, and would otherwise share
        // an anchor — so the second one gets a suffix, the way every other
        // documentation site does it.
        const base = slugify(plain) || `section-${headings.length}`;
        const count = seen.get(base) ?? 0;
        seen.set(base, count + 1);
        const id = count === 0 ? base : `${base}-${count}`;

        if (depth === 2 || depth === 3) {
          headings.push({ id, text: plain, level: depth });
        }
        return `<h${depth} id="${id}">${text}<a class="anchor" href="#${id}" aria-label="Link to this section">#</a></h${depth}>\n`;
      },

      code({ text, lang }) {
        const language = (lang ?? '').split(/\s/)[0];
        const known = language && hljs.getLanguage(language);
        const body = known
          ? hljs.highlight(text, { language }).value
          : text.replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' })[c] as string);

        // The language is shown rather than guessed at by the reader, and the
        // block scrolls on its own so a long signature never widens the page.
        return `<figure class="code"><span class="lang">${language || 'text'}</span><pre><code class="hljs">${body}</code></pre></figure>\n`;
      },

      link({ href, title, tokens }) {
        const text = this.parser.parseInline(tokens);
        // A page linking to a sibling writes /docs/@latest/..., because the
        // version in a documentation URL is the published API version and moves
        // with every release. Spelling it out meant rewriting a dozen links each
        // time and getting them wrong once, which is exactly the kind of chore
        // that should not be a chore.
        const resolved = href.replace(/^\/docs\/@latest\//, `/docs/${latestVersion}/`);
        const external = /^https?:\/\//.test(resolved);
        const attrs = external ? ' target="_blank" rel="noreferrer noopener"' : '';
        return `<a href="${resolved}"${title ? ` title="${title}"` : ''}${attrs}>${text}</a>`;
      },

      table({ header, rows }) {
        const head = header.map((cell) => `<th>${this.parser.parseInline(cell.tokens)}</th>`).join('');
        const body = rows
          .map((row) => `<tr>${row.map((cell) => `<td>${this.parser.parseInline(cell.tokens)}</td>`).join('')}</tr>`)
          .join('');
        // Wrapped so a wide table scrolls inside itself rather than pushing
        // the whole page sideways on a phone.
        return `<div class="table-scroll"><table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>\n`;
      },
    },
  });

  return { html: marked.parse(markdown) as string, headings };
}
