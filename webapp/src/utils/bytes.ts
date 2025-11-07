// src/utils/bytes.ts
// Vue-friendly util for parsing, summing and formatting byte sizes.
// Supports "512MB", "1 GiB", "750 MiB", "1.5 GB", "1024", etc.

export type ByteSizeInput = string | number;

const LETTER_INDEX: Record<string, number> = {
  B: 0, K: 1, M: 2, G: 3, T: 4, P: 5, E: 6, Z: 7, Y: 8,
};

const UNIT_RE = /^([0-9]*\.?[0-9]+)\s*([KMGTPEZY]?i?B?|[KMGTPEZY])$/i;

function parseUnit(unitRaw: string | undefined) {
  let unit = (unitRaw ?? 'B').trim().toUpperCase();
  if (!unit) unit = 'B';

  // Normalize shorthand like "M" → "MB"
  if (unit.length === 1 && unit !== 'B' && LETTER_INDEX[unit] !== undefined) {
    unit = unit + 'B';
  }

  const isIec = unit.includes('I') && unit !== 'B';
  const letter = unit[0] ?? 'B';
  const exp = LETTER_INDEX[letter] ?? 0;
  const base = isIec ? 1024 : 1000;

  return { base, exp };
}

export function toBytes(value: ByteSizeInput): number {
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) throw new Error('Number must be finite');
    return value; // already bytes
  }
  if (typeof value !== 'string') throw new TypeError('Expected string or number');

  const s = value.trim().replace(',', '.'); // support "1,5 GB"
  const m = s.match(UNIT_RE);
  if (!m) throw new Error(`Cannot parse size: "${value}"`);

  const num = parseFloat(m[1]);
  const { base, exp } = parseUnit(m[2]);
  return num * Math.pow(base, exp);
}

export function sumByteSizes(list: ByteSizeInput[]): number {
  return list.reduce((acc, v) => acc + toBytes(v), 0);
}

export type FormatOptions = {
  iec?: boolean;        // true → 1024 steps (KiB, MiB...), false → 1000 steps (KB, MB...)
  decimals?: number;    // number of decimals
  unit?: string;        // force a unit ("B","KB","MB","GB","KiB","MiB","GiB", ...)
};

const SI_UNITS = ['B','KB','MB','GB','TB','PB','EB','ZB','YB'] as const;
const IEC_UNITS = ['B','KiB','MiB','GiB','TiB','PiB','EiB','ZiB','YiB'] as const;

export function formatBytes(bytes: number, opts: FormatOptions = {}): string {
  if (!Number.isFinite(bytes)) throw new Error('Bytes must be finite');
  const iec = opts.iec ?? true;
  const decimals = opts.decimals ?? 2;

  const base = iec ? 1024 : 1000;
  const UNITS = iec ? IEC_UNITS : SI_UNITS;

  // If a unit is forced, use it
  if (opts.unit) {
    const u = opts.unit;
    // Map unit to exponent
    const isIec = /iB$/.test(u);
    const letter = u[0]?.toUpperCase() ?? 'B';
    const exp = LETTER_INDEX[letter] ?? 0;
    const forcedBase = isIec ? 1024 : 1000;
    const value = bytes / Math.pow(forcedBase, exp);
    return `${value.toFixed(decimals)} ${u}`;
  }

  if (Math.abs(bytes) < base) return `${bytes} B`;
  const i = Math.min(
    UNITS.length - 1,
    Math.floor(Math.log(Math.abs(bytes)) / Math.log(base))
  );
  const value = bytes / Math.pow(base, i);
  return `${value.toFixed(decimals)} ${UNITS[i]}`;
}

// Optional Vue composable wrapper (tree-shakable)
export function useBytes() {
  return { toBytes, sumByteSizes, formatBytes };
}
