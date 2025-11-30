// cityCodes.js

export const CITY_CODES : Record<string, string> = {
  // NL
  amsterdam: 'AMS',
  rotterdam: 'RTM',
  'the hague': 'HAG',
  'den haag': 'HAG',
  utrecht: 'UTC',
  eindhoven: 'EIN',

  // DE
  berlin: 'BER',
  frankfurt: 'FRA',
  hamburg: 'HAM',
  munich: 'MUC',
  dusseldorf: 'DUS',

  // FR / ES
  paris: 'PAR',
  lyon: 'LYO',
  marseille: 'MRS',
  madrid: 'MAD',
  barcelona: 'BCN',

  // UK / IE / CH / BE / LU / AT / CZ / PL
  london: 'LON',
  manchester: 'MAN',
  dublin: 'DUB',
  zurich: 'ZRH',
  geneva: 'GVA',
  brussels: 'BRU',
  brussel: 'BRU',
  luxembourg: 'LUX',
  vienna: 'VIE',
  wien: 'VIE',
  prague: 'PRG',
  praha: 'PRG',
  warsaw: 'WAW',
  warszawa: 'WAW',

  // Nordics / South Europe
  stockholm: 'STO',
  oslo: 'OSL',
  copenhagen: 'CPH',
  helsinki: 'HEL',
  lisbon: 'LIS',
  lisboa: 'LIS',
  milan: 'MIL',
  milano: 'MIL',
  rome: 'ROM',
  roma: 'ROM',
  budapest: 'BUD',
  bucharest: 'BUH',
  bucharesti: 'BUH',
  athens: 'ATH',
  athina: 'ATH',

  // US
  'new york': 'NYC',
  'new york city': 'NYC',
  ashburn: 'ASH',
  chicago: 'CHI',
  atlanta: 'ATL',
  miami: 'MIA',
  dallas: 'DAL',
  houston: 'HOU',
  denver: 'DEN',
  phoenix: 'PHX',
  'los angeles': 'LAX',
  'san francisco': 'SFO',
  seattle: 'SEA',
  portland: 'PDX',
  'las vegas': 'LAS',
  'san jose': 'SJC',

  // CA / MX / LATAM
  toronto: 'YTO',
  montreal: 'YMQ',
  vancouver: 'YVR',
  'mexico city': 'MEX',
  cdmx: 'MEX',
  'sao paulo': 'SAO',
  'rio de janeiro': 'RIO',
  'buenos aires': 'BUE',
  santiago: 'SCL',
  bogota: 'BOG',
  bogotá: 'BOG',
  lima: 'LIM',

  // East Asia
  tokyo: 'TYO',
  osaka: 'OSA',
  seoul: 'SEL',
  busan: 'PUS',
  beijing: 'BJS',
  shanghai: 'SHA',
  shenzhen: 'SZX',
  'hong kong': 'HKG',

  // SE Asia
  singapore: 'SIN',
  jakarta: 'JKT',
  'kuala lumpur': 'KUL',
  bangkok: 'BKK',
  manila: 'MNL',
  hanoi: 'HAN',
  'ho chi minh city': 'SGN',
  saigon: 'SGN',

  // Oceania
  sydney: 'SYD',
  melbourne: 'MEL',
  brisbane: 'BNE',
  perth: 'PER',
  auckland: 'AKL',

  // AFRICA
  johannesburg: 'JNB',
  'cape town': 'CPT',
  durban: 'DUR',
  lagos: 'LOS',
  nairobi: 'NBO',
  mombasa: 'MBA',
  'addis ababa': 'ADD',
  casablanca: 'CAS',
  rabat: 'RBA',
  cairo: 'CAI',
  alexandria: 'ALY',
  tunis: 'TUN',
  algiers: 'ALG',
  accra: 'ACC',
  abidjan: 'ABJ',
}

// Optioneel: normalizer en helper

export const normalizeCityName = (name: string) =>
  name
    .trim()
    .toLowerCase()
    .normalize('NFD') // split characters + accents
    .replace(/[\u0300-\u036f]/g, '') // remove all accents
    .replace(/[^a-z0-9\s]/g, '')    // remove punctuation and special chars

export default function getCityHub (name: string) {
  const key = normalizeCityName(name)
  return CITY_CODES[key] ?? null
}
