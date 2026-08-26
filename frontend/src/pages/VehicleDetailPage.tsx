import { Link, useParams } from 'react-router-dom'
import { useModelPriceHistory } from '../api/queries'
import { DepreciationChart } from '../components/DepreciationChart'
import { extractYear, formatYearLabel } from '../lib/year'

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

export function VehicleDetailPage() {
  const { modelId: modelIdParam } = useParams()
  const modelId = modelIdParam !== undefined ? Number(modelIdParam) : undefined
  const isValidModelId = modelId !== undefined && Number.isInteger(modelId) && modelId > 0

  const query = useModelPriceHistory(isValidModelId ? modelId : undefined)

  return (
    <div className="space-y-6">
      <Link to="/" className="text-sm text-blue-600 hover:underline">
        ← Voltar para a busca
      </Link>

      {!isValidModelId ? (
        <p className="text-sm text-red-600">Id de modelo inválido: {modelIdParam}</p>
      ) : query.isPending ? (
        <p className="text-sm text-slate-500">Carregando...</p>
      ) : query.isError ? (
        <p className="text-sm text-red-600">
          {query.error.message.includes('404')
            ? 'Modelo não encontrado.'
            : `Erro ao carregar o modelo: ${query.error.message}`}
        </p>
      ) : (
        <VehicleDetail data={query.data} />
      )}
    </div>
  )
}

function VehicleDetail({ data }: { data: NonNullable<ReturnType<typeof useModelPriceHistory>['data']> }) {
  const { brand, model, fipeCode, prices } = data

  if (prices.length === 0) {
    return (
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">
          {brand} {model}
        </h1>
        <p className="mt-2 text-sm text-slate-500">Sem dados de preço para este modelo.</p>
      </div>
    )
  }

  const priceValues = prices.map((p) => p.price)
  const minPrice = Math.min(...priceValues)
  const maxPrice = Math.max(...priceValues)

  const sortedPrices = [...prices].sort((a, b) => {
    const yearDiff = extractYear(a.yearCode) - extractYear(b.yearCode)
    return yearDiff !== 0 ? yearDiff : a.fuel.localeCompare(b.fuel)
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">
          {brand} {model}
        </h1>
        <p className="mt-1 text-sm text-slate-600">Código FIPE {fipeCode}</p>

        <div className="mt-3 flex gap-3">
          <SummaryChip label="Preço mínimo" value={currencyFormatter.format(minPrice)} />
          <SummaryChip label="Preço máximo" value={currencyFormatter.format(maxPrice)} />
        </div>
      </div>

      <DepreciationChart prices={prices} />

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              <th className="px-4 py-2 font-medium">Ano</th>
              <th className="px-4 py-2 font-medium">Combustível</th>
              <th className="px-4 py-2 text-right font-medium">Preço</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sortedPrices.map((point) => (
              <tr key={point.yearCode}>
                <td className="px-4 py-2">{formatYearLabel(extractYear(point.yearCode))}</td>
                <td className="px-4 py-2">{point.fuel}</td>
                <td className="px-4 py-2 text-right">{currencyFormatter.format(point.price)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function SummaryChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-slate-200 bg-white px-3 py-2">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="text-sm font-semibold text-slate-900">{value}</p>
    </div>
  )
}
