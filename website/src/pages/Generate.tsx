import { useEffect, useMemo, useState } from 'react';
import hljs from 'highlight.js/lib/core';
import {
  DEFAULT_OPTIONS,
  LICENSES,
  classOf,
  generate,
  packageOf,
  validate,
  type Features,
  type LicenseId,
  type Options,
} from '../lib/template';
import { platforms } from '../lib/platforms';
import { zip } from '../lib/zip';
import type { ZipEntry } from '../lib/zip';

/** What each toggle adds, in the order a mod author meets them. */
const FEATURES: { id: keyof Features; label: string; blurb: string }[] = [
  {
    id: 'content',
    label: 'A block and an item',
    blurb: 'Registered through a Registrar, in a creative tab, with placeholder textures.',
  },
  {
    id: 'ember',
    label: 'Data generation',
    blurb: 'Ember writes the models, names, loot tables and recipes from the Java above.',
  },
  {
    id: 'client',
    label: 'A client source set',
    blurb: 'src/client, where rendering and key bindings go. A dedicated server never loads it.',
  },
  {
    id: 'config',
    label: 'Configuration',
    blurb: 'Settings as a record: the field names are the JSON keys and the types are the checking.',
  },
  {
    id: 'commands',
    label: 'A command',
    blurb: 'Registered through the event bus, so it survives a datapack reload.',
  },
  {
    id: 'networking',
    label: 'Networking',
    blurb: 'One payload each way, with the codec that puts it on the wire.',
  },
  {
    id: 'mixins',
    label: 'A mixin',
    blurb: 'For the cases the API has no event for. Comes with the rules that make one work.',
  },
  {
    id: 'ci',
    label: 'GitHub Actions',
    blurb: 'A workflow that builds the mod on every push and keeps the jar.',
  },
];

/** Groups the flat file list into something that reads like a directory tree. */
function tree(files: ZipEntry[]): { dir: string; entries: ZipEntry[] }[] {
  const groups = new Map<string, ZipEntry[]>();
  for (const file of [...files].sort((a, b) => a.path.localeCompare(b.path))) {
    const at = file.path.lastIndexOf('/');
    const dir = at === -1 ? '' : file.path.slice(0, at);
    const list = groups.get(dir);
    if (list) {
      list.push(file);
    } else {
      groups.set(dir, [file]);
    }
  }
  return [...groups.entries()]
    .sort(([a], [b]) => (a === '' ? -1 : b === '' ? 1 : a.localeCompare(b)))
    .map(([dir, entries]) => ({ dir, entries }));
}

const LANGUAGES: Record<string, string> = {
  java: 'java',
  kts: 'kotlin',
  json: 'json',
  properties: 'properties',
  md: 'markdown',
  yml: 'yaml',
};

export function Generate() {
  const [options, setOptions] = useState<Options>(DEFAULT_OPTIONS);
  const [files, setFiles] = useState<ZipEntry[]>([]);
  const [selected, setSelected] = useState<string>('build.gradle.kts');
  const [building, setBuilding] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);

  const errors = useMemo(() => validate(options), [options]);
  const valid = Object.keys(errors).length === 0;

  // The preview is the real thing: the same call the download makes, so what a
  // visitor reads before downloading cannot differ from what they get.
  useEffect(() => {
    if (!valid) {
      return;
    }
    let live = true;
    generate(options)
      .then((built) => {
        if (live) {
          setFiles(built);
          setFailure(null);
        }
      })
      .catch((error: unknown) => {
        if (live) {
          setFailure(error instanceof Error ? error.message : String(error));
        }
      });
    return () => {
      live = false;
    };
  }, [options, valid]);

  const set = <K extends keyof Options>(key: K, value: Options[K]) =>
    setOptions((previous) => ({ ...previous, [key]: value }));

  const toggle = (id: keyof Features) =>
    setOptions((previous) => ({
      ...previous,
      features: { ...previous.features, [id]: !previous.features[id] },
    }));

  async function download() {
    setBuilding(true);
    try {
      const built = await generate(options);
      const url = URL.createObjectURL(zip(built));
      const link = document.createElement('a');
      link.href = url;
      link.download = `${options.modId}.zip`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      setFailure(error instanceof Error ? error.message : String(error));
    } finally {
      setBuilding(false);
    }
  }

  const shown = files.find((file) => file.path === selected) ?? files[0];
  const extension = shown?.path.split('.').pop() ?? '';
  const language = LANGUAGES[extension];
  const highlighted =
    shown?.text && language && hljs.getLanguage(language)
      ? hljs.highlight(shown.text, { language }).value
      : null;

  return (
    <div className="generate shell">
      <div className="generate-head">
        <p className="eyebrow">
          <span />
          New project
        </p>
        <h1>Start a mod</h1>
        <p>
          Pick what you need and take the zip. Everything it generates compiles and runs as it
          comes — placeholder textures included, so the first launch shows a block rather than the
          missing-texture checker.
        </p>
      </div>

      <div className="generate-body">
        <form className="generate-form" onSubmit={(event) => event.preventDefault()}>
          <label>
            <span>Mod name</span>
            <input
              value={options.modName}
              onChange={(event) => set('modName', event.target.value)}
            />
            {errors.modName && <p className="field-error">{errors.modName}</p>}
          </label>

          <label>
            <span>Mod id</span>
            <input value={options.modId} onChange={(event) => set('modId', event.target.value)} />
            {errors.modId && <p className="field-error">{errors.modId}</p>}
          </label>

          <label>
            <span>Group</span>
            <input value={options.group} onChange={(event) => set('group', event.target.value)} />
            {errors.group ? (
              <p className="field-error">{errors.group}</p>
            ) : (
              <p className="field-error" style={{ color: 'var(--faint)' }}>
                Package: {packageOf(options)} · class: {classOf(options)}
              </p>
            )}
          </label>

          <label>
            <span>Version</span>
            <input
              value={options.version}
              onChange={(event) => set('version', event.target.value)}
            />
            {errors.version && <p className="field-error">{errors.version}</p>}
          </label>

          <label>
            <span>Author</span>
            <input
              value={options.author}
              placeholder="Your name"
              onChange={(event) => set('author', event.target.value)}
            />
          </label>

          <label>
            <span>Description</span>
            <input
              value={options.description}
              onChange={(event) => set('description', event.target.value)}
            />
          </label>

          <label>
            <span>Minecraft</span>
            <select
              value={options.minecraft}
              onChange={(event) => set('minecraft', event.target.value)}
            >
              {platforms.map((platform) => (
                <option key={platform.minecraft} value={platform.minecraft}>
                  {platform.minecraft}
                  {platform.status === 'current' ? '' : ` — ${platform.status}`}
                </option>
              ))}
            </select>
          </label>

          <label>
            <span>Licence</span>
            <select
              value={options.license}
              onChange={(event) => set('license', event.target.value as LicenseId)}
            >
              {LICENSES.map((licence) => (
                <option key={licence.id} value={licence.id}>
                  {licence.label}
                </option>
              ))}
            </select>
          </label>

          <fieldset className="options">
            <legend>What to include</legend>
            {FEATURES.map((feature) => (
              <label className="check" key={feature.id}>
                <input
                  type="checkbox"
                  checked={options.features[feature.id]}
                  disabled={feature.id === 'ember' && !options.features.content}
                  onChange={() => toggle(feature.id)}
                />
                <span>
                  <strong>{feature.label}</strong>
                  <em>
                    {feature.id === 'ember' && !options.features.content
                      ? 'Needs something to generate for — turn on the block and item.'
                      : feature.blurb}
                  </em>
                </span>
              </label>
            ))}
          </fieldset>

          <button
            type="button"
            className="button primary download"
            disabled={!valid || building}
            onClick={download}
          >
            {building ? 'Building…' : `Download ${options.modId}.zip`}
          </button>

          {failure ? (
            <p className="field-error">{failure}</p>
          ) : (
            <p className="generate-next">
              Unzip it, then <code>./gradlew runClient</code>
            </p>
          )}
        </form>

        <div className="generate-preview">
          <div className="tree">
            {tree(files).map((group) => (
              <div key={group.dir}>
                <div className="tree-dir">{group.dir === '' ? '.' : `${group.dir}/`}</div>
                {group.entries.map((file) => (
                  <button
                    type="button"
                    key={file.path}
                    className={`tree-file${file.path === shown?.path ? ' selected' : ''}`}
                    onClick={() => setSelected(file.path)}
                  >
                    {'  '}
                    {file.path.split('/').pop()}
                    {file.data ? ' · binary' : ''}
                  </button>
                ))}
              </div>
            ))}
          </div>

          <figure className="code preview-file">
            <span className="lang">{shown?.path ?? ''}</span>
            <pre>
              {shown?.data ? (
                <code>
                  {shown.data.length} bytes. Binary files are in the zip but not shown here.
                </code>
              ) : highlighted ? (
                <code dangerouslySetInnerHTML={{ __html: highlighted }} />
              ) : (
                <code>{shown?.text ?? ''}</code>
              )}
            </pre>
          </figure>
        </div>
      </div>
    </div>
  );
}
