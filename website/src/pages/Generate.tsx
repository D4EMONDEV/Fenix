import { useEffect, useMemo, useState } from 'react';
import hljs from '../lib/highlight';
import {
  DEFAULT_OPTIONS,
  LICENSES,
  classOf,
  generate,
  idFromName,
  packageOf,
  validate,
  type Features,
  type LicenseId,
  type Options,
} from '../lib/template';
import { platforms } from '../lib/platforms';
import { treeRows } from '../lib/tree';
import { zip } from '../lib/zip';
import type { ZipEntry } from '../lib/zip';

const FEATURES: { id: keyof Features; label: string; blurb: string }[] = [
  {
    id: 'starterContent',
    label: 'Starter content',
    blurb:
      'A block, an item and a creative tab, with the classes that own them. Off, you get the entry point and empty resource folders.',
  },
  {
    id: 'ember',
    label: 'Ember generators',
    blurb:
      'Write the models, names, loot tables and recipes as Java instead of JSON. Ember is wired into every project either way; this adds generators to start from.',
  },
  {
    id: 'splitClient',
    label: 'Split main and client',
    blurb:
      'Add a src/client source set. Code a dedicated server never runs lives there, and the compiler stops the common half reaching for it.',
  },
  {
    id: 'kotlin',
    label: 'Kotlin build script',
    blurb: 'The Gradle build script will use the Kotlin programming language instead of Groovy.',
  },
];

const LANGUAGES: Record<string, string> = {
  java: 'java',
  kts: 'kotlin',
  gradle: 'groovy',
  json: 'json',
  properties: 'properties',
  md: 'markdown',
};

export function Generate() {
  const [options, setOptions] = useState<Options>(DEFAULT_OPTIONS);
  const [files, setFiles] = useState<ZipEntry[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
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

  // The id follows the name until somebody takes it over, and picks the thread
  // back up if they hand it back.
  const setName = (modName: string) =>
    setOptions((previous) => ({
      ...previous,
      modName,
      modId: previous.autoModId ? idFromName(modName) : previous.modId,
    }));

  const setAutoId = (autoModId: boolean) =>
    setOptions((previous) => ({
      ...previous,
      autoModId,
      modId: autoModId ? idFromName(previous.modName) : previous.modId,
    }));

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

  const rows = useMemo(
    () => treeRows(files, options.modId),
    [files, options.modId],
  );

  const shown =
    files.find((file) => file.path === selected)
    ?? files.find((file) => file.path.endsWith(options.features.kotlin ? 'build.gradle.kts' : 'build.gradle'))
    ?? files[0];
  const extension = shown?.path.split('.').pop() ?? '';
  const language = LANGUAGES[extension];
  const highlighted =
    shown?.text && language && hljs.getLanguage(language)
      ? hljs.highlight(shown.text, { language }).value
      : null;

  return (
    <div className="generate shell">
      <div className="generate-head">
        <h1>Start a mod</h1>
        <p>
          A block, an item and a creative tab, in a project that builds and runs as it comes.
          Everything else is a guide away.
        </p>
      </div>

      <div className="generate-body">
        <form className="generate-form" onSubmit={(event) => event.preventDefault()}>
          <label>
            <span>Mod name</span>
            <input value={options.modName} onChange={(event) => setName(event.target.value)} />
            {errors.modName && <p className="field-error">{errors.modName}</p>}
          </label>

          <div className="field">
            <div className="field-head">
              <span>Mod id</span>
              <label className="inline-check">
                <input
                  type="checkbox"
                  checked={options.autoModId}
                  onChange={(event) => setAutoId(event.target.checked)}
                />
                from the name
              </label>
            </div>
            <input
              value={options.modId}
              disabled={options.autoModId}
              onChange={(event) => set('modId', event.target.value)}
            />
            {errors.modId && <p className="field-error">{errors.modId}</p>}
          </div>

          <label>
            <span>Group</span>
            <input value={options.group} onChange={(event) => set('group', event.target.value)} />
            {errors.group ? (
              <p className="field-error">{errors.group}</p>
            ) : (
              <p className="field-note">
                {packageOf(options)}.{classOf(options)}
              </p>
            )}
          </label>

          <div className="field-pair">
            <label>
              <span>Version</span>
              <input
                value={options.version}
                onChange={(event) => set('version', event.target.value)}
              />
              {errors.version && <p className="field-error">{errors.version}</p>}
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
          </div>

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
                  onChange={() => toggle(feature.id)}
                />
                <span>
                  <strong>{feature.label}</strong>
                  <em>
                    {feature.id === 'ember' && !options.features.starterContent
                      ? 'No starter content to generate for, so you get the empty package and the ember task, ready for your own.'
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
            {rows.map((row) =>
              row.directory ? (
                <div className="tree-row tree-dir" key={`d:${row.prefix}${row.name}`}>
                  <span className="tree-prefix">{row.prefix}</span>
                  {row.name}
                </div>
              ) : (
                <button
                  type="button"
                  key={row.path}
                  className={`tree-row tree-file${row.path === shown?.path ? ' selected' : ''}`}
                  onClick={() => setSelected(row.path)}
                >
                  <span className="tree-prefix">{row.prefix}</span>
                  {row.name}
                </button>
              ),
            )}
          </div>

          <figure className="code preview-file">
            <span className="lang">{shown?.path ?? ''}</span>
            <pre>
              {shown?.data ? (
                <code>{shown.data.length} bytes. Binary, so not shown here.</code>
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
