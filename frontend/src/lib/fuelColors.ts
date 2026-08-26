/**
 * Ordem fixa por combustível (nunca por posição/rank) — paleta categórica
 * validada da skill dataviz. Um combustível sempre tem a mesma cor entre
 * telas diferentes (gráfico de depreciação, distribuição de combustível em
 * Insights etc.); um combustível fora dessa lista cai no fallback.
 */
export const FUEL_ORDER = [
  'Gasolina',
  'Diesel',
  'Flex',
  'Álcool',
  'Elétrico',
  'Híbrido',
  'Gás Natural',
]

const FUEL_COLOR_VARS: Record<string, string> = {
  Gasolina: 'var(--fuel-gasolina)',
  Diesel: 'var(--fuel-diesel)',
  Flex: 'var(--fuel-flex)',
  Álcool: 'var(--fuel-alcool)',
  Elétrico: 'var(--fuel-eletrico)',
  Híbrido: 'var(--fuel-hibrido)',
  'Gás Natural': 'var(--fuel-gas-natural)',
}

const FALLBACK_COLOR_VAR = 'var(--chart-text-muted)'

export function fuelColor(fuel: string): string {
  return FUEL_COLOR_VARS[fuel] ?? FALLBACK_COLOR_VAR
}

/** Ordena uma lista de combustíveis vistos pela ordem fixa, com desconhecidos ao final. */
export function orderFuels(fuelsSeen: Iterable<string>): string[] {
  const seen = new Set(fuelsSeen)
  const ordered = FUEL_ORDER.filter((f) => seen.has(f))
  for (const f of seen) {
    if (!ordered.includes(f)) {
      ordered.push(f)
    }
  }
  return ordered
}
