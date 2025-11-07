import { describe, it, expect } from 'vitest'
import { toBytes, sumByteSizes, formatBytes, type ByteSizeInput } from '@/utils/bytes'

// Helper voor benadering
const approx = (a: number, b: number, eps = 1e-9) => Math.abs(a - b) < eps

describe('toBytes', () => {
  it('interprets numbers as bytes', () => {
    expect(toBytes(0)).toBe(0)
    expect(toBytes(1024)).toBe(1024)
  })

  it('parses SI units (1000-based)', () => {
    expect(toBytes('1 KB')).toBe(1000)
    expect(toBytes('1MB')).toBe(1000 ** 2)
    expect(toBytes('2 GB')).toBe(2 * 1000 ** 3)
    expect(toBytes('1 TB')).toBe(1000 ** 4)
  })

  it('parses IEC units (1024-based)', () => {
    expect(toBytes('1 KiB')).toBe(1024)
    expect(toBytes('1MiB')).toBe(1024 ** 2)
    expect(toBytes('2 GiB')).toBe(2 * 1024 ** 3)
    expect(toBytes('1 TiB')).toBe(1024 ** 4)
  })

  it('supports shorthand without B (M -> MB)', () => {
    expect(toBytes('1 M')).toBe(1000 ** 2)
    expect(toBytes('1 G')).toBe(1000 ** 3)
  })

  it('supports decimals and comma notation', () => {
    expect(approx(toBytes('1.5 GB'), 1.5 * 1000 ** 3)).toBe(true)
    expect(approx(toBytes('1,5 GB'), 1.5 * 1000 ** 3)).toBe(true)
  })

  it('accepts values without unit as bytes', () => {
    expect(toBytes('512')).toBe(512)
  })

  it('throws on invalid input', () => {
    expect(() => toBytes('abc')).toThrowError()
    expect(() => toBytes('1 XB')).toThrowError()
  })
})

describe('sumByteSizes', () => {
  it('sums an empty list to 0', () => {
    expect(sumByteSizes([])).toBe(0)
  })

  it('sums mixed inputs (string and number)', () => {
    const list: ByteSizeInput[] = ['1 KB', 500, '0.5 KiB']
    const expected = 1000 + 500 + 512
    expect(approx(sumByteSizes(list), expected)).toBe(true)
  })

  it('sums large values correctly', () => {
    const list: ByteSizeInput[] = ['1 GiB', '512 MiB', '0.5 GB']
    const expected = 1024 ** 3 + 512 * 1024 ** 2 + 0.5 * 1000 ** 3
    expect(approx(sumByteSizes(list), expected)).toBe(true)
  })
})

describe('formatBytes', () => {
  it('formats in IEC with 2 decimals by default', () => {
    expect(formatBytes(0)).toBe('0.00 B')
    expect(formatBytes(1023)).toBe('1023.00 B')
    expect(formatBytes(1024)).toBe('1.00 KiB')
    expect(formatBytes(1024 ** 2)).toBe('1.00 MiB')
  })

  it('formats in SI when iec=false', () => {
    expect(formatBytes(1000, { iec: false })).toBe('1.00 KB')
    expect(formatBytes(1000 ** 2, { iec: false })).toBe('1.00 MB')
  })

  it('forced unit uses correct base and exponent', () => {
    expect(formatBytes(1024, { unit: 'B' })).toBe('1024.00 B')
    expect(formatBytes(1024, { unit: 'KiB' })).toBe('1.00 KiB')
    expect(formatBytes(1000, { unit: 'KB' })).toBe('1.00 KB')
    expect(formatBytes(1.5 * 1000 ** 3, { unit: 'GB', decimals: 1 })).toBe('1.5 GB')
  })

  it('respects decimals option', () => {
    expect(formatBytes(1024 ** 2 + 512 * 1024, { decimals: 3 })).toBe('1.500 MiB')
  })

  it('throws for non-finite values', () => {
    expect(() => formatBytes(Number.NaN)).toThrowError()
    expect(() => formatBytes(Number.POSITIVE_INFINITY)).toThrowError()
  })
})
