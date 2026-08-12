/**
 * Turns a flat list of paths into the rows of a directory tree.
 *
 * A list of files grouped under their directory tells you what is in the zip
 * but not how it nests, and `src/main/java/com/example/mymod/content` as a
 * header is a path to read rather than a shape to see. The connectors are the
 * whole point: they say at a glance which directory something is in and where
 * one ends.
 */

export interface TreeRow {
  /** The connector column, e.g. `│   ├── `. Monospace, and already padded. */
  prefix: string;
  /** The last segment: a file name, or a directory name with a trailing slash. */
  name: string;
  /** The full path, for a file; empty for a directory. */
  path: string;
  directory: boolean;
}

interface Node {
  name: string;
  path: string;
  children: Map<string, Node>;
  /** Set on leaves only. */
  file?: unknown;
}

function insert(root: Node, path: string, file: unknown) {
  const segments = path.split('/');
  let node = root;
  segments.forEach((segment, at) => {
    let child = node.children.get(segment);
    if (!child) {
      child = {
        name: segment,
        path: segments.slice(0, at + 1).join('/'),
        children: new Map(),
      };
      node.children.set(segment, child);
    }
    node = child;
  });
  node.file = file;
}

/**
 * Folds a chain of directories that each hold only one directory into a single
 * row: `com/ example/ mymod/` becomes `com/example/mymod/`.
 *
 * Java's package layout is most of the depth of a mod project and none of its
 * shape. Left expanded, six of the twelve rows under `src/main/java` are one
 * name apiece and the files sit so far right they wrap. This is what an IDE's
 * "compact middle packages" does, for the same reason.
 *
 * @return the deepest node the chain reaches, and the name to show for it
 */
function fold(node: Node): { node: Node; name: string } {
  let deepest = node;
  let name = node.name;
  while (deepest.children.size === 1) {
    const only = [...deepest.children.values()][0];
    // A directory holding one file is not a chain; the file is the content.
    if (only.children.size === 0) {
      break;
    }
    name += `/${only.name}`;
    deepest = only;
  }
  return { node: deepest, name };
}

/** Directories first, then files; alphabetical within each. */
function ordered(node: Node): Node[] {
  return [...node.children.values()].sort((a, b) => {
    const aDir = a.children.size > 0;
    const bDir = b.children.size > 0;
    if (aDir !== bDir) {
      return aDir ? -1 : 1;
    }
    return a.name.localeCompare(b.name);
  });
}

/**
 * {@return the rows to draw, in order}
 *
 * @param paths every file in the project
 * @param root  the name to show at the top, usually the mod id
 */
export function treeRows(paths: string[], root: string): TreeRow[] {
  const top: Node = { name: root, path: '', children: new Map() };
  for (const path of paths) {
    insert(top, path, true);
  }

  const rows: TreeRow[] = [{ prefix: '', name: `${root}/`, path: '', directory: true }];

  // `ancestors` holds, for each level above this one, whether that level's node
  // was the last of its siblings — which decides whether its column is drawn as
  // a continuing line or as blank space.
  const walk = (node: Node, ancestors: boolean[]) => {
    const children = ordered(node);
    children.forEach((child, at) => {
      const last = at === children.length - 1;
      const prefix = ancestors.map((done) => (done ? '    ' : '│   ')).join('')
          + (last ? '└── ' : '├── ');
      if (child.children.size === 0) {
        rows.push({ prefix, name: child.name, path: child.path, directory: false });
        return;
      }
      const folded = fold(child);
      rows.push({ prefix, name: `${folded.name}/`, path: '', directory: true });
      walk(folded.node, [...ancestors, last]);
    });
  };

  walk(top, []);
  return rows;
}
