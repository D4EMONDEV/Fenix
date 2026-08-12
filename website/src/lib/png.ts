/**
 * A PNG encoder, for the placeholder textures a generated project ships with.
 *
 * A block with no texture is not a block that looks unfinished — it is the
 * magenta-and-black checker, which reads as a broken mod rather than as one
 * waiting for art. The generated project would launch and show exactly the
 * failure a first-time mod author cannot tell apart from a mistake they made.
 *
 * So the generator draws something. Sixteen by sixteen, one colour with a
 * little noise so the faces of a cube are distinguishable, and a border so the
 * edges of a block read at a distance.
 *
 * Stored deflate blocks rather than real compression, for the same reason the
 * zip writer beside this uses them: the whole image is a kilobyte, and every
 * decoder in existence handles them.
 */

/** Reused by the zip writer's CRC-32; PNG wants the same polynomial. */
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

/** The checksum zlib puts after the deflate stream. */
function adler32(bytes: Uint8Array): number {
  let a = 1;
  let b = 0;
  for (const byte of bytes) {
    a = (a + byte) % 65521;
    b = (b + a) % 65521;
  }
  return ((b << 16) | a) >>> 0;
}

function be32(value: number): Uint8Array {
  return new Uint8Array([
    (value >>> 24) & 0xff,
    (value >>> 16) & 0xff,
    (value >>> 8) & 0xff,
    value & 0xff,
  ]);
}

function concat(parts: Uint8Array[]): Uint8Array {
  const total = parts.reduce((sum, part) => sum + part.length, 0);
  const out = new Uint8Array(total);
  let at = 0;
  for (const part of parts) {
    out.set(part, at);
    at += part.length;
  }
  return out;
}

/** A PNG chunk: length, type, payload, CRC over type and payload. */
function chunk(type: string, payload: Uint8Array): Uint8Array {
  const name = new TextEncoder().encode(type);
  const body = concat([name, payload]);
  return concat([be32(payload.length), body, be32(crc32(body))]);
}

/** Wraps raw bytes as a zlib stream of stored (uncompressed) deflate blocks. */
function zlib(raw: Uint8Array): Uint8Array {
  const blocks: Uint8Array[] = [];
  const MAX = 0xffff;
  for (let at = 0; at < raw.length; at += MAX) {
    const slice = raw.subarray(at, Math.min(at + MAX, raw.length));
    const last = at + MAX >= raw.length ? 1 : 0;
    const length = slice.length;
    blocks.push(new Uint8Array([
      last,
      length & 0xff,
      (length >>> 8) & 0xff,
      ~length & 0xff,
      (~length >>> 8) & 0xff,
    ]));
    blocks.push(slice);
  }
  // 0x78 0x01: deflate, 32K window, no preset dictionary, fastest setting.
  return concat([new Uint8Array([0x78, 0x01]), ...blocks, be32(adler32(raw))]);
}

/** A deterministic pseudo-random source, so the same options give the same file. */
function noise(seed: number): () => number {
  let state = seed >>> 0 || 1;
  return () => {
    state ^= state << 13;
    state ^= state >>> 17;
    state ^= state << 5;
    return (state >>> 0) / 0xffffffff;
  };
}

/**
 * {@return a 16×16 RGBA PNG in the given colour}
 *
 * @param hex   the base colour, `0xRRGGBB`
 * @param round whether to round the corners, which is what makes an item read
 *              as an item rather than as a very small block
 */
export function texture(hex: number, round = false): Uint8Array {
  const size = 16;
  const base = [(hex >> 16) & 0xff, (hex >> 8) & 0xff, hex & 0xff];
  const random = noise(hex);

  // One filter byte per row, then RGBA per pixel. Filter 0 is "none", which
  // costs a byte a row and saves having to reproduce the filters on decode.
  const raw = new Uint8Array(size * (1 + size * 4));
  let at = 0;
  for (let y = 0; y < size; y++) {
    raw[at++] = 0;
    for (let x = 0; x < size; x++) {
      const corner = round && (x + y < 3 || x - y > 12 || y - x > 12 || x + y > 26);
      if (corner) {
        raw[at++] = 0;
        raw[at++] = 0;
        raw[at++] = 0;
        raw[at++] = 0;
        continue;
      }
      // The border is darker, so the edges of a block are visible against
      // another of the same block.
      const edge = x === 0 || y === 0 || x === size - 1 || y === size - 1;
      const shade = (edge ? -38 : 0) + Math.round((random() - 0.5) * 26);
      raw[at++] = Math.max(0, Math.min(255, base[0] + shade));
      raw[at++] = Math.max(0, Math.min(255, base[1] + shade));
      raw[at++] = Math.max(0, Math.min(255, base[2] + shade));
      raw[at++] = 255;
    }
  }

  const header = concat([
    be32(size),
    be32(size),
    // 8 bits per channel, colour type 6 (RGBA), deflate, no filter, no interlace.
    new Uint8Array([8, 6, 0, 0, 0]),
  ]);

  return concat([
    new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', header),
    chunk('IDAT', zlib(raw)),
    chunk('IEND', new Uint8Array(0)),
  ]);
}
