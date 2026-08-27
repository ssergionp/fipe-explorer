import { Fragment, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  useCalendarHistory,
  useModelPriceHistory,
  usePriceEstimate,
  useSavePriceEstimate,
  useVehicleConditions,
  useVehicleExtras,
} from '../api/queries'
import type { PriceEstimateResponse, VehicleCondition, VehicleType } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { CalendarHistoryChart } from '../components/CalendarHistoryChart'
import { DepreciationChart } from '../components/DepreciationChart'
import { FavoriteButton } from '../components/FavoriteButton'
import { WatchVehicleButton } from '../components/WatchVehicleButton'
import { extractYear, formatYearLabel } from '../lib/year'

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const percentFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'percent',
  signDisplay: 'exceptZero',
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

export function VehicleDetailPage() {
  const { modelId: modelIdParam } = useParams()
  const modelId = modelIdParam !== undefined ? Number(modelIdParam) : undefined
  const isValidModelId = modelId !== undefined && Number.isInteger(modelId) && modelId > 0

  const query = useModelPriceHistory(isValidModelId ? modelId : undefined)

  return (
    <div className="space-y-6">
      <Link to="/" className="text-sm text-blue-600 underline">
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
  const { brand, model, vehicleType, fipeCode, prices } = data
  const [expandedHistoryYearCode, setExpandedHistoryYearCode] = useState<string | null>(null)
  const [expandedEstimatePriceEntryId, setExpandedEstimatePriceEntryId] = useState<number | null>(null)

  if (prices.length === 0) {
    return (
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">
          {brand} {model}
        </h1>
        <p className="mt-1 text-sm text-slate-600">Código FIPE {fipeCode}</p>
        <p className="mt-2 text-sm text-slate-500">Sem dados de preço para este modelo.</p>
        <div className="mt-3">
          <WatchVehicleButton fipeCode={fipeCode} />
        </div>
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

        <div className="mt-3 flex flex-wrap items-end gap-3">
          <SummaryChip label="Preço mínimo" value={currencyFormatter.format(minPrice)} />
          <SummaryChip label="Preço máximo" value={currencyFormatter.format(maxPrice)} />
          <WatchVehicleButton fipeCode={fipeCode} />
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
              <th className="px-4 py-2 text-right font-medium">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sortedPrices.map((point) => {
              const isHistoryExpanded = expandedHistoryYearCode === point.yearCode
              const isEstimateExpanded = expandedEstimatePriceEntryId === point.priceEntryId
              return (
                <Fragment key={point.yearCode}>
                  <tr>
                    <td className="px-4 py-2">{formatYearLabel(extractYear(point.yearCode))}</td>
                    <td className="px-4 py-2">{point.fuel}</td>
                    <td className="px-4 py-2 text-right">{currencyFormatter.format(point.price)}</td>
                    <td className="px-4 py-2 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <FavoriteButton priceEntryId={point.priceEntryId} />
                        <button
                          type="button"
                          onClick={() => setExpandedHistoryYearCode(isHistoryExpanded ? null : point.yearCode)}
                          className="rounded-md border border-slate-300 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50"
                        >
                          {isHistoryExpanded ? 'Ocultar histórico' : 'Ver histórico real de preço'}
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            setExpandedEstimatePriceEntryId(isEstimateExpanded ? null : point.priceEntryId)
                          }
                          className="rounded-md border border-slate-300 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50"
                        >
                          {isEstimateExpanded ? 'Ocultar estimativa' : 'Estimar valor real deste veículo'}
                        </button>
                      </div>
                    </td>
                  </tr>
                  {(isHistoryExpanded || isEstimateExpanded) && (
                    <tr>
                      <td colSpan={4} className="space-y-4 bg-slate-50 px-4 py-4">
                        {isHistoryExpanded && (
                          <CalendarHistoryPanel
                            vehicleType={vehicleType}
                            fipeCode={fipeCode}
                            yearCode={point.yearCode}
                            fuel={point.fuel}
                          />
                        )}
                        {isEstimateExpanded && <PriceEstimateForm priceEntryId={point.priceEntryId} />}
                      </td>
                    </tr>
                  )}
                </Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function CalendarHistoryPanel({
  vehicleType,
  fipeCode,
  yearCode,
  fuel,
}: {
  vehicleType: VehicleType
  fipeCode: string
  yearCode: string
  fuel: string
}) {
  const query = useCalendarHistory(vehicleType, fipeCode, yearCode)

  if (query.isPending) {
    return <p className="text-sm text-slate-500">Carregando histórico real de preço...</p>
  }

  if (query.isError) {
    return <p className="text-sm text-red-600">Erro ao carregar histórico: {query.error.message}</p>
  }

  const { status, reason, months, cached } = query.data

  if (status !== 'AVAILABLE') {
    return (
      <p className="text-sm text-amber-700">
        Histórico real indisponível no momento{reason ? `: ${reason}` : '.'}
      </p>
    )
  }

  if (months.length === 0) {
    return <p className="text-sm text-slate-500">Sem histórico mensal disponível para esta combinação.</p>
  }

  return (
    <div>
      <CalendarHistoryChart months={months} fuel={fuel} />
      {cached && <p className="mt-1 text-xs text-slate-400">Resultado em cache (atualizado nas últimas 24h).</p>}
    </div>
  )
}

function PriceEstimateForm({ priceEntryId }: { priceEntryId: number }) {
  const conditionsQuery = useVehicleConditions()
  const extrasQuery = useVehicleExtras()
  const mutation = usePriceEstimate(priceEntryId)

  const [km, setKm] = useState('')
  const [condition, setCondition] = useState<VehicleCondition | ''>('')
  const [selectedExtras, setSelectedExtras] = useState<string[]>([])

  function toggleExtra(key: string) {
    setSelectedExtras((prev) => (prev.includes(key) ? prev.filter((extra) => extra !== key) : [...prev, key]))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const kmValue = Number(km)
    if (condition === '' || !Number.isFinite(kmValue) || kmValue < 0) {
      return
    }
    mutation.mutate({ km: kmValue, condition, extras: selectedExtras })
  }

  return (
    <div className="space-y-3">
      <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-4 rounded-lg border border-slate-200 bg-white p-4">
        <label className="flex flex-col text-xs font-medium text-slate-600">
          Quilometragem
          <input
            type="number"
            min={0}
            step={1}
            required
            value={km}
            onChange={(event) => setKm(event.target.value)}
            className="mt-1 w-32 rounded-md border border-slate-300 px-2 py-1 text-sm text-slate-900"
          />
        </label>

        <label className="flex flex-col text-xs font-medium text-slate-600">
          Estado de conservação
          <select
            required
            value={condition}
            onChange={(event) => setCondition(event.target.value as VehicleCondition)}
            className="mt-1 rounded-md border border-slate-300 px-2 py-1 text-sm text-slate-900"
          >
            <option value="" disabled>
              Selecione...
            </option>
            {conditionsQuery.data?.map((option) => (
              <option key={option.key} value={option.key}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <fieldset className="flex flex-col text-xs font-medium text-slate-600">
          <legend>Opcionais</legend>
          <div className="mt-1 flex max-w-md flex-wrap gap-x-3 gap-y-1">
            {extrasQuery.data?.map((extra) => (
              <label key={extra.key} className="flex items-center gap-1 text-sm font-normal text-slate-700">
                <input
                  type="checkbox"
                  checked={selectedExtras.includes(extra.key)}
                  onChange={() => toggleExtra(extra.key)}
                />
                {extra.label}
              </label>
            ))}
          </div>
        </fieldset>

        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-md bg-slate-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
        >
          {mutation.isPending ? 'Calculando...' : 'Calcular valor estimado'}
        </button>
      </form>

      {mutation.isError && <p className="text-sm text-red-600">Erro ao calcular: {mutation.error.message}</p>}

      {mutation.isSuccess && (
        <PriceEstimateResult
          priceEntryId={priceEntryId}
          km={Number(km)}
          condition={condition as VehicleCondition}
          extras={selectedExtras}
          result={mutation.data}
        />
      )}
    </div>
  )
}

function PriceEstimateResult({
  priceEntryId,
  km,
  condition,
  extras,
  result,
}: {
  priceEntryId: number
  km: number
  condition: VehicleCondition
  extras: string[]
  result: PriceEstimateResponse
}) {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const saveMutation = useSavePriceEstimate()

  function handleSave() {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    saveMutation.mutate({ priceEntryId, km, condition, extras })
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-center gap-3">
        <SummaryChip label="Preço FIPE (base)" value={currencyFormatter.format(result.basePrice)} />
        <SummaryChip label="Valor estimado" value={currencyFormatter.format(result.adjustedPrice)} />
        <button
          type="button"
          onClick={handleSave}
          disabled={saveMutation.isPending || saveMutation.isSuccess}
          className="ml-auto rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          {saveMutation.isSuccess
            ? 'Estimativa salva ✓'
            : saveMutation.isPending
              ? 'Salvando...'
              : 'Salvar esta estimativa'}
        </button>
      </div>

      {saveMutation.isError && (
        <p className="mt-2 text-sm text-red-600">Erro ao salvar: {saveMutation.error.message}</p>
      )}

      <ul className="mt-3 space-y-1 text-sm">
        {result.components.map((component) => {
          const color =
            component.amount > 0 ? 'text-emerald-700' : component.amount < 0 ? 'text-red-700' : 'text-slate-500'
          const sign = component.amount > 0 ? '+' : ''
          return (
            <li key={component.key} className="flex items-center justify-between gap-3">
              <span className="text-slate-600">{component.label}</span>
              <span className={`whitespace-nowrap font-medium ${color}`}>
                {sign}
                {currencyFormatter.format(component.amount)} ({percentFormatter.format(component.percent)})
              </span>
            </li>
          )
        })}
      </ul>

      <p className="mt-3 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-800">
        Isto é uma estimativa automática, não uma avaliação profissional — use como referência, não
        como garantia de valor de venda ou compra.
      </p>
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
