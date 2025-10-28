// Maak de lijst duidelijk als constante en exporteer voor hergebruik
const VESSEL_NAMES: readonly string[] = [
  'Boaty McBoatface',
  'Titanic',
  'HMS Victory',
  'USS Constitution',
  'Mayflower',
  'Santa Maria',
  'Queen Mary',
  'Rotterdam',
  'Black Pearl',
  'Jolly Roger',
  'Jenny',
  'Lady Washington',
  'Nautilus',
  'Pequod',
  'SS Venture',
  'Orca',
  'Royal Fortune',
  'Flying Dutchman',
  'Black Stallion',
  'Scourge of the Seas',
  'Stormbringer'
]

// Publieke API met duidelijke naam en functie-declaratie
export default function vesselNameGenerator(): string {
  const index = Math.floor(Math.random() * VESSEL_NAMES.length)
  return VESSEL_NAMES[index ?? 0] ?? 'Boaty McBoatface'
}
