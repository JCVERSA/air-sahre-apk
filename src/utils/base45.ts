const BASE45_CHARSET = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:';
const BASE45_LOOKUP = new Int16Array(256).fill(-1);

for (let i = 0; i < BASE45_CHARSET.length; i++) {
  BASE45_LOOKUP[BASE45_CHARSET.charCodeAt(i)] = i;
}

export function uint8ArrayToBase45(bytes: Uint8Array): string {
  let result = '';
  const len = bytes.length;
  let i = 0;

  while (i < len) {
    if (i + 1 < len) {
      const val = (bytes[i] << 8) | bytes[i + 1];
      const c = Math.floor(val / (45 * 45));
      const rem = val % (45 * 45);
      const d = Math.floor(rem / 45);
      const e = rem % 45;
      result += BASE45_CHARSET[e] + BASE45_CHARSET[d] + BASE45_CHARSET[c];
      i += 2;
    } else {
      const val = bytes[i];
      const d = Math.floor(val / 45);
      const e = val % 45;
      result += BASE45_CHARSET[e] + BASE45_CHARSET[d];
      i += 1;
    }
  }

  return result;
}

export function base45ToUint8Array(str: string): Uint8Array {
  const len = str.length;
  const out: number[] = [];
  let i = 0;

  while (i < len) {
    if (i + 2 < len) {
      const e = BASE45_LOOKUP[str.charCodeAt(i)];
      const d = BASE45_LOOKUP[str.charCodeAt(i + 1)];
      const c = BASE45_LOOKUP[str.charCodeAt(i + 2)];

      if (e === -1 || d === -1 || c === -1) {
        throw new Error('Invalid Base45 character');
      }

      const val = e + d * 45 + c * 45 * 45;
      out.push((val >> 8) & 0xff);
      out.push(val & 0xff);
      i += 3;
    } else if (i + 1 < len) {
      const e = BASE45_LOOKUP[str.charCodeAt(i)];
      const d = BASE45_LOOKUP[str.charCodeAt(i + 1)];

      if (e === -1 || d === -1) {
        throw new Error('Invalid Base45 character');
      }

      const val = e + d * 45;
      out.push(val & 0xff);
      i += 2;
    } else {
      throw new Error('Invalid Base45 string length');
    }
  }

  return new Uint8Array(out);
}

export async function compressData(data: Uint8Array): Promise<{ compressed: Uint8Array; wasCompressed: boolean }> {
  if (typeof CompressionStream === 'undefined' || data.byteLength < 256) {
    return { compressed: data, wasCompressed: false };
  }

  try {
    const cs = new CompressionStream('gzip');
    const writer = cs.writable.getWriter();
    writer.write(data as unknown as BufferSource);
    writer.close();

    const response = new Response(cs.readable);
    const arrayBuffer = await response.arrayBuffer();
    const compressed = new Uint8Array(arrayBuffer);

    if (compressed.byteLength < data.byteLength * 0.95) {
      return { compressed, wasCompressed: true };
    }
  } catch (err) {
    console.warn('Compression skipped:', err);
  }

  return { compressed: data, wasCompressed: false };
}

export async function decompressData(data: Uint8Array): Promise<Uint8Array> {
  if (typeof DecompressionStream === 'undefined') {
    return data;
  }

  try {
    const ds = new DecompressionStream('gzip');
    const writer = ds.writable.getWriter();
    writer.write(data as unknown as BufferSource);
    writer.close();

    const response = new Response(ds.readable);
    const arrayBuffer = await response.arrayBuffer();
    return new Uint8Array(arrayBuffer);
  } catch (err) {
    console.warn('Decompression failed or data was uncompressed:', err);
    return data;
  }
}
