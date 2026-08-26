const ZERO_KM_YEAR = 32000

export function extractYear(yearCode: string): number {
  const [prefix] = yearCode.split('-')
  return Number(prefix)
}

export function formatYearLabel(year: number): string {
  return year === ZERO_KM_YEAR ? 'Zero KM' : String(year)
}
