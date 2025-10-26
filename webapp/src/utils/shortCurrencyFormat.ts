export default function shortCurrencyFormat(value: number) {
  const hasDecimals = !Number.isInteger(value)
  return new Intl.NumberFormat('nl-NL', {
    minimumFractionDigits: hasDecimals ? 2 : 0,
    maximumFractionDigits: hasDecimals ? 2 : 0,
  }).format(value)
}
