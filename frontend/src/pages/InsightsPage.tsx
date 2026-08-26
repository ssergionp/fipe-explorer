import { useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useFuelDistribution, useStatsSummary, useTopBrands } from '../api/queries'
import type { FuelDistributionEntry, TopBrand, VehicleType } from '../api/types'
import { fuelColor, orderFuels } from '../lib/fuelColors'

const VEHICLE_TYPE_LABELS: Record<VehicleType, string> = {
  CAR: 'Carro',
  MOTORCYCLE: 'Moto',
  TRUCK: 'Caminhão',
}

const VEHICLE_TYPES: VehicleType[] = ['CAR', 'MOTORCYCLE', 'TRUCK']

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

const integerFormatter = new Intl.NumberFormat('pt-BR')

export function InsightsPage() {
  const [type, setType] = useState<VehicleType | undefined>(undefined)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Insights</h1>
        <p className="mt-1 text-sm text-slate-600">
          Estatísticas e rankings sobre a Tabela FIPE por tipo de veículo.
        </p>
      </div>

      <div className="flex gap-2">
        {VEHICLE_TYPES.map((vt) => (
          <button
            key={vt}
            type="button"
            onClick={() => setType(vt)}
            className={`rounded-md px-3 py-1.5 text-sm font-medium ${
              type === vt
                ? 'bg-slate-900 text-white'
                : 'border border-slate-300 text-slate-700 hover:bg-slate-50'
            }`}
          >
            {VEHICLE_TYPE_LABELS[vt]}
          </button>
        ))}
      </div>

      {type === undefined ? (
        <p className="text-sm text-slate-500">Selecione um tipo de veículo para ver as estatísticas.</p>
      ) : (
        <>
          <SummarySection type={type} />
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <TopBrandsSection type={type} order="desc" title="10 marcas mais caras" barColor="var(--stat-bar-high)" />
            <TopBrandsSection type={type} order="asc" title="10 marcas mais baratas" barColor="var(--stat-bar-low)" />
          </div>
          <FuelDistributionSection type={type} />
        </>
      )}
    </div>
  )
}

function SummarySection({ type }: { type: VehicleType }) {
  const query = useStatsSummary(type)

  if (query.isPending) {
    return <p className="text-sm text-slate-500">Carregando resumo...</p>
  }

  if (query.isError) {
    return <p className="text-sm text-red-600">Erro ao carregar resumo: {query.error.message}</p>
  }

  const data = query.data

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
      <StatCard label="Modelos" value={integerFormatter.format(data.distinctModels)} />
      <StatCard
        label="Faixa de preço"
        value={`${compactCurrencyFormatter.format(data.minPrice)} – ${compactCurrencyFormatter.format(data.maxPrice)}`}
      />
      <StatCard label="Preço médio" value={currencyFormatter.format(data.avgPrice)} />
      <StatCard label="Registros históricos" value={integerFormatter.format(data.totalPriceEntries)} />
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="mt-1 text-lg font-semibold text-slate-900">{value}</p>
    </div>
  )
}

function TopBrandsSection({
  type,
  order,
  title,
  barColor,
}: {
  type: VehicleType
  order: 'asc' | 'desc'
  title: string
  barColor: string
}) {
  const query = useTopBrands(type, order, 10)

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <h2 className="text-sm font-semibold text-slate-900">{title}</h2>
      {query.isPending ? (
        <p className="mt-3 text-sm text-slate-500">Carregando...</p>
      ) : query.isError ? (
        <p className="mt-3 text-sm text-red-600">Erro ao carregar: {query.error.message}</p>
      ) : query.data.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">Sem dados suficientes.</p>
      ) : (
        <TopBrandsChart data={query.data} barColor={barColor} />
      )}
    </div>
  )
}

function TopBrandsChart({ data, barColor }: { data: TopBrand[]; barColor: string }) {
  const rows = data.map((brand) => ({
    ...brand,
    label: `${brand.brandName} (${brand.modelCount})`,
  }))

  return (
    <div style={{ height: Math.max(rows.length * 32, 120) }} className="mt-3">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={rows} layout="vertical" margin={{ top: 4, right: 48, bottom: 4, left: 4 }}>
          <CartesianGrid horizontal={false} stroke="var(--chart-grid)" />
          <XAxis type="number" hide />
          <YAxis
            type="category"
            dataKey="label"
            width={170}
            tick={{ fill: 'var(--chart-text-secondary)', fontSize: 12 }}
            axisLine={{ stroke: 'var(--chart-axis)' }}
            tickLine={false}
          />
          <Tooltip
            cursor={{ fill: 'var(--chart-grid)' }}
            formatter={(value) => currencyFormatter.format(Number(value))}
            labelFormatter={() => ''}
          />
          <Bar dataKey="avgPrice" fill={barColor} radius={[0, 4, 4, 0]} barSize={18} isAnimationActive={false}>
            <LabelList
              dataKey="avgPrice"
              position="right"
              formatter={(label) => compactCurrencyFormatter.format(Number(label))}
              fill="var(--chart-text-secondary)"
              fontSize={12}
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

function FuelDistributionSection({ type }: { type: VehicleType }) {
  const query = useFuelDistribution(type)

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <h2 className="text-sm font-semibold text-slate-900">Distribuição por combustível</h2>
      {query.isPending ? (
        <p className="mt-3 text-sm text-slate-500">Carregando...</p>
      ) : query.isError ? (
        <p className="mt-3 text-sm text-red-600">Erro ao carregar: {query.error.message}</p>
      ) : query.data.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">Sem dados.</p>
      ) : (
        <FuelDistributionChart data={query.data} />
      )}
    </div>
  )
}

function FuelDistributionChart({ data }: { data: FuelDistributionEntry[] }) {
  const total = data.reduce((sum, entry) => sum + entry.count, 0)
  const order = orderFuels(data.map((entry) => entry.fuel))
  const rows = order
    .map((fuel) => data.find((entry) => entry.fuel === fuel))
    .filter((entry): entry is FuelDistributionEntry => entry !== undefined)

  return (
    <div style={{ height: Math.max(rows.length * 32, 120) }} className="mt-3">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={rows} layout="vertical" margin={{ top: 4, right: 64, bottom: 4, left: 4 }}>
          <CartesianGrid horizontal={false} stroke="var(--chart-grid)" />
          <XAxis type="number" hide />
          <YAxis
            type="category"
            dataKey="fuel"
            width={110}
            tick={{ fill: 'var(--chart-text-secondary)', fontSize: 12 }}
            axisLine={{ stroke: 'var(--chart-axis)' }}
            tickLine={false}
          />
          <Tooltip
            cursor={{ fill: 'var(--chart-grid)' }}
            formatter={(value) => integerFormatter.format(Number(value))}
            labelFormatter={() => ''}
          />
          <Bar dataKey="count" radius={[0, 4, 4, 0]} barSize={18} isAnimationActive={false}>
            {rows.map((entry) => (
              <Cell key={entry.fuel} fill={fuelColor(entry.fuel)} />
            ))}
            <LabelList
              dataKey="count"
              position="right"
              formatter={(label) => {
                const value = Number(label)
                return `${integerFormatter.format(value)} (${((value / total) * 100).toFixed(0)}%)`
              }}
              fill="var(--chart-text-secondary)"
              fontSize={12}
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
