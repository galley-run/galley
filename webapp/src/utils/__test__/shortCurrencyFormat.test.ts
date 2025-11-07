// shortCurrencyFormat.test.ts

import { describe, expect, it } from 'vitest'
import shortCurrencyFormat from '@/utils/shortCurrencyFormat'

describe('shortCurrencyFormat', () => {
  it('formats integers without decimals', () => {
    const result = shortCurrencyFormat(1234)
    expect(result).toBe('1.234')
  })

  it('formats decimal numbers with two fraction digits', () => {
    const result = shortCurrencyFormat(1234.56)
    expect(result).toBe('1.234,56')
  })

  it('formats numbers with trailing zeros in decimals', () => {
    const result = shortCurrencyFormat(1234.5)
    expect(result).toBe('1.234,50')
  })

  it('formats small decimal numbers', () => {
    const result = shortCurrencyFormat(0.12)
    expect(result).toBe('0,12')
  })

  it('formats large numbers correctly', () => {
    const result = shortCurrencyFormat(123456789.99)
    expect(result).toBe('123.456.789,99')
  })

  it('formats zero as "0"', () => {
    const result = shortCurrencyFormat(0)
    expect(result).toBe('0')
  })
})
