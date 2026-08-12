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
  /**
   * Set when the entry itself said it was a directory.
   *
   * <p>Having children is the usual proof, and it is not enough: a project can
   * ship an empty folder on purpose, and one inferred from its children alone
   * would be drawn as a file.
   */
  empty?: boolean;
}

function insert(root: Node, path: string, directory: boolean) {
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
  if (directory) {
    node.empty = true;
  }
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
function isDirectory(node: Node): boolean {
  return node.children.size > 0 || !!node.empty;
}

function fold(node: Node): { node: Node; name: string } {
  let deepest = node;
  let name = node.name;
  while (deepest.children.size === 1 && !deepest.empty) {
    const only = [...deepest.children.values()][0];
    // A directory holding one file is not a chain; the file is the content. An
    // empty one is, though — `assets/my-mod/` reads better than two rows.
    if (!isDirectory(only)) {
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
    const aDir = isDirectory(a);
    const bDir = isDirectory(b);
    if (aDir !== bDir) {
      return aDir ? -1 : 1;
    }
    return a.name.localeCompare(b.name);
  });
}

/**
 * {@return the rows to draw, in order}
 *
 * @param entries every file in the project, and any folder meant to stay empty
 * @param root    the name to show at the top, usually the mod id
 */
export function treeRows(
  entries: { path: string; directory?: boolean }[],
  root: string,
): TreeRow[] {
  const top: Node = { name: root, path: '', children: new Map() };
  for (const entry of entries) {
    insert(top, entry.path, !!entry.directory);
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
      if (!isDirectory(child)) {
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
