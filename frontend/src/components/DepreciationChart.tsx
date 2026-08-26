import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  type TooltipContentProps,
} from 'recharts'
import type { PricePoint } from '../api/types'
import { fuelColor, orderFuels } from '../lib/fuelColors'
import { extractYear, formatYearLabel } from '../lib/year'

type ChartRow = { year: number; yearLabel: string } & Record<string, number | string>

function buildChartData(prices: PricePoint[]): { rows: ChartRow[]; fuels: string[] } {
  const rowsByYear = new Map<number, ChartRow>()
  const fuelsSeen = new Set<string>()

  for (const point of prices) {
    const year = extractYear(point.yearCode)
    fuelsSeen.add(point.fuel)
    const row = rowsByYear.get(year) ?? { year, yearLabel: formatYearLabel(year) }
    row[point.fuel] = point.price
    rowsByYear.set(year, row)
  }

  const rows = [...rowsByYear.values()].sort((a, b) => a.year - b.year)
  return { rows, fuels: orderFuels(fuelsSeen) }
}

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const compactCurrencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1,
})

function ChartTooltip({ active, payload, label }: TooltipContentProps) {
  if (!active || !payload) {
    return null
  }

  const visible = payload.filter((entry) => entry.value !== undefined && entry.value !== null)
  if (visible.length === 0) {
    return null
  }

  return (
    <div className="rounded-md border border-slate-200 bg-white px-3 py-2 text-sm shadow-md">
      <p className="font-medium text-slate-900">{label}</p>
      <ul className="mt-1 space-y-1">
        {visible.map((entry) => (
          <li key={String(entry.dataKey)} className="flex items-center gap-2">
            <span className="inline-block h-0.5 w-4" style={{ backgroundColor: entry.color }} />
            <span className="text-slate-600">{entry.name}</span>
            <span className="ml-auto font-semibold text-slate-900">
              {currencyFormatter.format(entry.value as number)}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

export function DepreciationChart({ prices }: { prices: PricePoint[] }) {
  const { rows, fuels } = buildChartData(prices)
  // Modelos com muitos anos (ex.: 30+) lotam o eixo X se todo tick for exibido;
  // mostra no máximo ~12 rótulos, espaçados igualmente, preservando as pontas.
  const tickInterval = rows.length > 12 ? Math.ceil(rows.length / 12) - 1 : 0

  return (
    <div className="h-80 w-full rounded-lg border border-slate-200 bg-white p-4">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={rows} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid vertical={false} stroke="var(--chart-grid)" />
          <XAxis
            dataKey="yearLabel"
            interval={tickInterval}
            tick={{ fill: 'var(--chart-text-muted)', fontSize: 12 }}
            axisLine={{ stroke: 'var(--chart-axis)' }}
            tickLine={false}
          />
          <YAxis
            tickFormatter={(value: number) => compactCurrencyFormatter.format(value)}
            tick={{ fill: 'var(--chart-text-muted)', fontSize: 12 }}
            axisLine={{ stroke: 'var(--chart-axis)' }}
            tickLine={false}
            width={64}
          />
          <Tooltip
            content={ChartTooltip}
            cursor={{ stroke: 'var(--chart-axis)', strokeWidth: 1 }}
          />
          {fuels.length > 1 && (
            <Legend
              wrapperStyle={{ fontSize: 12, color: 'var(--chart-text-secondary)' }}
              iconType="line"
            />
          )}
          {fuels.map((fuel) => (
            <Line
              key={fuel}
              type="linear"
              dataKey={fuel}
              name={fuel}
              stroke={fuelColor(fuel)}
              strokeWidth={2}
              dot={{ r: 4, strokeWidth: 2, stroke: 'var(--chart-surface)', fill: fuelColor(fuel) }}
              activeDot={{ r: 5, strokeWidth: 2, stroke: 'var(--chart-surface)' }}
              connectNulls={false}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
