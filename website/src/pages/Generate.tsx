import { useMemo, useState } from 'react';
import {
  DEFAULTS,
  FENIX_VERSION,
  MINECRAFT_VERSION,
  WRAPPER,
  classFromName,
  generate,
  idFromName,
  problems,
  wrapper,
  type Options,
} from '../lib/template';
import { zip } from '../lib/zip';

/** A file tree, built from the flat paths the template produced. */
interface Node {
  name: string;
  path?: string;
  children: Node[];
}

function tree(paths: string[]): Node {
  const root: Node = { name: '', children: [] };

  for (const path of paths) {
    let at = root;
    const parts = path.split('/');
    parts.forEach((part, index) => {
      const leaf = index === parts.length - 1;
      let next = at.children.find((child) => child.name === part && !child.path === !leaf);
      if (!next) {
        next = { name: part, children: [], ...(leaf ? { path } : {}) };
        at.children.push(next);
      }
      at = next;
    });
  }
  // Directories before files, then alphabetical — the order a file manager
  // shows, so the shape is the one the reader expects on disk.
  const sort = (node: Node) => {
    node.children.sort(
      (a, b) => Number(!!a.path) - Number(!!b.path) || a.name.localeCompare(b.name),
    );
    node.children.forEach(sort);
  };
  sort(root);
  return root;
}

function Branch({
  node,
  depth,
  selected,
  onSelect,
}: {
  node: Node;
  depth: number;
  selected: string;
  onSelect: (path: string) => void;
}) {
  return (
    <>
      {node.children.map((child) => (
        <div key={child.name + (child.path ?? '/')}>
          {child.path ? (
            <button
              type="button"
              className={`tree-file${child.path === selected ? ' selected' : ''}`}
              style={{ paddingLeft: `${depth * 0.9 + 0.6}rem` }}
              onClick={() => onSelect(child.path!)}
            >
              {child.name}
            </button>
          ) : (
            <div className="tree-dir" style={{ paddingLeft: `${depth * 0.9 + 0.6}rem` }}>
              {child.name}/
            </div>
          )}
          {child.children.length > 0 && (
            <Branch node={child} depth={depth + 1} selected={selected} onSelect={onSelect} />
          )}
        </div>
      ))}
    </>
  );
}

/**
 * What the preview says about a file it cannot show.
 *
 * The wrapper is in the archive and so it is in the tree; showing a jar as text
 * would be worse than showing nothing, and leaving it out of the tree would
 * misdescribe what the download contains.
 */
const COPIED = `Copied into the archive when you download it, not generated here.

This is the Gradle wrapper. It is why the README can say ./gradlew rather than
asking you to install Gradle first: the wrapper fetches the exact version this
project was written for, and everyone who builds it uses that same one.

Keep all four files, and keep them in version control.`;

export function Generate() {
  const [options, setOptions] = useState<Options>(DEFAULTS);
  // Once either is typed into, it stops following the name — otherwise a later
  // edit to the name silently overwrites what the author chose.
  const [touched, setTouched] = useState<{ id: boolean; className: boolean }>({
    id: false,
    className: false,
  });
  const [selected, setSelected] = useState('');
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState('');

  const files = useMemo(() => generate(options), [options]);
  const invalid = useMemo(() => problems(options), [options]);
  const ok = Object.keys(invalid).length === 0;

  // Everything the archive will hold, generated and copied alike.
  const listed = useMemo(
    () => [
      ...files.map((file) => ({ path: file.path, text: file.text ?? '' })),
      ...WRAPPER.map((file) => ({ path: `${options.modId}/${file.path}`, text: COPIED })),
    ],
    [files, options.modId],
  );

  const shown = listed.find((file) => file.path === selected) ?? listed[0];
  const root = useMemo(() => tree(listed.map((file) => file.path)), [listed]);

  function set<K extends keyof Options>(key: K, value: Options[K]) {
    setOptions((current) => ({ ...current, [key]: value }));
  }

  function setName(name: string) {
    setOptions((current) => ({
      ...current,
      name,
      modId: touched.id ? current.modId : idFromName(name),
      className: touched.className ? current.className : classFromName(name),
    }));
  }

  async function download() {
    setBusy(true);
    setFailed('');
    try {
      // The wrapper is fetched rather than generated, so this is the one part
      // that can fail. Better to say so than to hand out an archive that is
      // quietly missing the file its README opens with.
      const blob = zip([...files, ...(await wrapper(options.modId))]);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${options.modId}.zip`;
      link.click();
      // Revoked on the next turn of the loop: the click is synchronous but the
      // browser reads the blob afterwards, and revoking too early downloads
      // nothing.
      setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (error) {
      setFailed(error instanceof Error ? error.message : String(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="generate shell">
      <header className="generate-head">
        <p className="eyebrow">Fenix project generator</p>
        <h1>Start with your idea.</h1>
        <p>
          A clean project for Minecraft <strong>{MINECRAFT_VERSION}</strong> and Fenix{' '}
          <strong>{FENIX_VERSION}</strong>. No sample blocks, no placeholder items, no code to
          delete. Everything is made in your browser.
        </p>
      </header>

      <div className="generate-body">
        <form className="generate-form" onSubmit={(event) => event.preventDefault()}>
          <label>
            <span>Mod name</span>
            <input value={options.name} onChange={(event) => setName(event.target.value)} />
            {invalid.name && <em className="field-error">{invalid.name}</em>}
          </label>

          <label>
            <span>Mod id</span>
            <input
              value={options.modId}
              onChange={(event) => {
                setTouched((t) => ({ ...t, id: true }));
                set('modId', event.target.value);
              }}
            />
            {invalid.modId && <em className="field-error">{invalid.modId}</em>}
          </label>

          <label>
            <span>Description</span>
            <input
              value={options.description}
              onChange={(event) => set('description', event.target.value)}
            />
          </label>

          <label>
            <span>Package</span>
            <input
              value={options.packageName}
              onChange={(event) => set('packageName', event.target.value)}
            />
            {invalid.packageName && <em className="field-error">{invalid.packageName}</em>}
          </label>

          <label>
            <span>Main class</span>
            <input
              value={options.className}
              onChange={(event) => {
                setTouched((t) => ({ ...t, className: true }));
                set('className', event.target.value);
              }}
            />
            {invalid.className && <em className="field-error">{invalid.className}</em>}
          </label>

          <label>
            <span>Author</span>
            <input
              value={options.author}
              placeholder="optional"
              onChange={(event) => set('author', event.target.value)}
            />
          </label>

          <label>
            <span>Licence</span>
            <select value={options.license} onChange={(event) => set('license', event.target.value)}>
              <option value="Apache-2.0">Apache-2.0</option>
              <option value="MIT">MIT</option>
              <option value="GPL-3.0-only">GPL-3.0-only</option>
              <option value="LGPL-3.0-only">LGPL-3.0-only</option>
              <option value="none">Not stated</option>
            </select>
          </label>

          <fieldset className="options">
            <legend>Options</legend>

            <label className="check">
              <input
                type="checkbox"
                checked={options.ember}
                onChange={(event) => set('ember', event.target.checked)}
              />
              <span>
                <strong>Ember generator</strong>
                <em>A blank resource generator, ready when you need generated assets or data.</em>
              </span>
            </label>

            <label className="check">
              <input
                type="checkbox"
                checked={options.client}
                onChange={(event) => set('client', event.target.checked)}
              />
              <span>
                <strong>Client source set</strong>
                <em>
                  <code>src/client/java</code>, which a dedicated server never loads.
                </em>
              </span>
            </label>

          </fieldset>

          <button
            className="button primary download"
            type="button"
            disabled={!ok || busy}
            onClick={download}
          >
            {!ok ? 'Fix the fields above' : busy ? 'Packing…' : `Download ${options.modId}.zip`}
          </button>

          {failed && <p className="field-error">Could not build the archive: {failed}</p>}

          <p className="generate-next">
            Then <code>./gradlew runClient</code>. The Gradle wrapper is in the archive and the
            plugin downloads the game itself, so that is the only command you need.
          </p>
        </form>

        <section className="generate-preview" aria-label="Project preview">
          <div className="tree">
            <Branch node={root} depth={0} selected={shown.path} onSelect={setSelected} />
          </div>

          <figure className="code preview-file">
            <span className="lang">{shown.path.split('/').pop()}</span>
            <pre>
              <code>{shown.text}</code>
            </pre>
          </figure>
        </section>
      </div>
    </div>
  );
}
