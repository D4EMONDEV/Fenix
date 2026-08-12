/**
 * A zip writer, in about a hundred lines.
 *
 * Stored, not deflated. A generated project is a dozen small text files —
 * eight kilobytes before compression, six after — and the difference is not
 * worth a compression library in the bundle. Every zip reader in existence
 * handles stored entries; it is the oldest thing in the format.
 */

const encoder = new TextEncoder();

/**
 * One file, on its way into an archive.
 *
 * Either text or bytes — the Gradle wrapper is a jar, and a project that cannot
 * be built is not a project.
 */
export interface ZipEntry {
  path: string;
  text?: string;
  data?: Uint8Array;
  /** Marks the entry executable, for the shell script that starts a build. */
  executable?: boolean;
  /**
   * An empty directory rather than a file.
   *
   * <p>A generated project can want a folder with nothing in it — somewhere to
   * put textures, marked out so nobody has to work out where they go. A zip
   * records those as their own entries, named with a trailing slash and no
   * content; without one the folder simply is not there after extraction,
   * because a zip has no other notion of a directory.
   *
   * <p>Git does not track empty directories, so one committed as-is disappears
   * on the next clone. That is git's business, not the archive's.
   */
  directory?: boolean;
}

/**
 * CRC-32, as the zip format wants it.
 *
 * The table is built once on first use: computing it is 256 × 8 shifts, and
 * doing that per file would be slower than the archive it serves.
 */
const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let i = 0; i < 256; i++) {
    let value = i;
    for (let bit = 0; bit < 8; bit++) {
      value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
    }
    table[i] = value >>> 0;
  }
  return table;
})();

function crc32(bytes: Uint8Array): number {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc = CRC_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

/** Grows as it is written to, so nothing has to know the size in advance. */
class Buffer {
  private bytes = new Uint8Array(1024);
  private length = 0;

  private room(extra: number) {
    if (this.length + extra <= this.bytes.length) {
      return;
    }
    let size = this.bytes.length * 2;
    while (size < this.length + extra) {
      size *= 2;
    }
    const grown = new Uint8Array(size);
    grown.set(this.bytes.subarray(0, this.length));
    this.bytes = grown;
  }

  u16(value: number) {
    this.room(2);
    this.bytes[this.length++] = value & 0xff;
    this.bytes[this.length++] = (value >>> 8) & 0xff;
  }

  u32(value: number) {
    this.room(4);
    this.bytes[this.length++] = value & 0xff;
    this.bytes[this.length++] = (value >>> 8) & 0xff;
    this.bytes[this.length++] = (value >>> 16) & 0xff;
    this.bytes[this.length++] = (value >>> 24) & 0xff;
  }

  raw(data: Uint8Array) {
    this.room(data.length);
    this.bytes.set(data, this.length);
    this.length += data.length;
  }

  get size() {
    return this.length;
  }

  done(): Uint8Array {
    return this.bytes.subarray(0, this.length);
  }
}

/** {@return the entries as a zip archive} */
export function zip(entries: ZipEntry[]): Blob {
  const out = new Buffer();
  const directory: {
    name: Uint8Array;
    crc: number;
    size: number;
    offset: number;
    executable: boolean;
    directory: boolean;
  }[] = [];

  for (const entry of entries) {
    // A directory entry is named with a trailing slash; that slash is the only
    // thing that makes it one.
    const path = entry.directory && !entry.path.endsWith('/') ? `${entry.path}/` : entry.path;
    const name = encoder.encode(path);
    // CRLF for text, because a generated project is opened on Windows more
    // often than not and a file that is one long line in Notepad looks broken.
    // Bytes are written exactly as they arrived: a jar rewritten line by line
    // is not a jar, and `gradlew` is read by a shell that stops at the first
    // carriage return it does not expect.
    const data =
      entry.data ?? encoder.encode((entry.text ?? '').replace(/\r?\n/g, '\r\n'));
    const crc = crc32(data);
    const offset = out.size;

    out.u32(0x04034b50); // local file header
    out.u16(20); // version needed: 2.0
    out.u16(0x0800); // flags: the name is UTF-8
    out.u16(0); // method: stored
    out.u16(0); // modified time
    out.u16(0x21); // modified date — 1 January 1980, the epoch zip was given
    out.u32(crc);
    out.u32(data.length); // compressed size
    out.u32(data.length); // uncompressed size
    out.u16(name.length);
    out.u16(0); // extra field length
    out.raw(name);
    out.raw(data);

    directory.push({
      name,
      crc,
      size: data.length,
      offset,
      executable: !!entry.executable,
      directory: !!entry.directory,
    });
  }

  const directoryStart = out.size;
  for (const file of directory) {
    out.u32(0x02014b50); // central directory header
    out.u16(0x031e); // version made by: Unix, so the permissions above are read
    out.u16(20); // version needed
    out.u16(0x0800);
    out.u16(0);
    out.u16(0);
    out.u16(0x21);
    out.u32(file.crc);
    out.u32(file.size);
    out.u32(file.size);
    out.u16(file.name.length);
    out.u16(0); // extra
    out.u16(0); // comment
    out.u16(0); // disk number
    out.u16(0); // internal attributes
    // Unix permissions live in the top sixteen bits. Without them `gradlew`
    // arrives unreadable by the shell on macOS and Linux, and the first thing
    // the README asks for fails. The low bits are MS-DOS attributes, where 0x10
    // marks a directory — Windows Explorer reads those and not the Unix mode.
    const mode = file.directory ? 0o040755 : file.executable ? 0o100755 : 0o100644;
    out.u32((((mode << 16) >>> 0) | (file.directory ? 0x10 : 0)) >>> 0);
    out.u32(file.offset);
    out.raw(file.name);
  }

  // Measured before the trailer is written, not during. Asking the buffer for
  // its size afterwards counts the trailer's own bytes as part of the
  // directory, and a reader that trusts the number walks off the end of it —
  // "bad magic number for central directory", from an archive that looks fine
  // in a file manager lenient enough to scan for the signature instead.
  const directorySize = out.size - directoryStart;

  out.u32(0x06054b50); // end of central directory
  out.u16(0); // this disk
  out.u16(0); // disk with the directory
  out.u16(directory.length);
  out.u16(directory.length);
  out.u32(directorySize);
  out.u32(directoryStart);
  out.u16(0); // comment length

  return new Blob([out.done() as unknown as BlobPart], { type: 'application/zip' });
}
