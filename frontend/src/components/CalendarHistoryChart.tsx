import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  type TooltipContentProps,
} from 'recharts'
import type { CalendarHistoryPoint } from '../api/types'
import { fuelColor } from '../lib/fuelColors'

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
  if (!active || !payload || payload.length === 0) {
    return null
  }
  const entry = payload[0]

  return (
    <div className="rounded-md border border-slate-200 bg-white px-3 py-2 text-sm shadow-md">
      <p className="font-medium text-slate-900">{label}</p>
      <p className="font-semibold text-slate-900">{currencyFormatter.format(entry.value as number)}</p>
    </div>
  )
}

/** Uma única série (o ano/combustível já foi fixado pela linha da tabela que abriu o painel). */
export function CalendarHistoryChart({ months, fuel }: { months: CalendarHistoryPoint[]; fuel: string }) {
  // Mesma lógica de espaçamento de ticks do gráfico de depreciação, só que num range menor
  // (meses em vez de anos).
  const tickInterval = months.length > 8 ? Math.ceil(months.length / 8) - 1 : 0
  const color = fuelColor(fuel)

  return (
    <div className="h-56 w-full rounded-lg border border-slate-200 bg-white p-4">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={months} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid vertical={false} stroke="var(--chart-grid)" />
          <XAxis
            dataKey="month"
            interval={tickInterval}
            tick={{ fill: 'var(--chart-text-muted)', fontSize: 11 }}
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
          <Tooltip content={ChartTooltip} cursor={{ stroke: 'var(--chart-axis)', strokeWidth: 1 }} />
          <Line
            type="linear"
            dataKey="price"
            name={fuel}
            stroke={color}
            strokeWidth={2}
            dot={{ r: 3, strokeWidth: 2, stroke: 'var(--chart-surface)', fill: color }}
            activeDot={{ r: 5, strokeWidth: 2, stroke: 'var(--chart-surface)' }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
