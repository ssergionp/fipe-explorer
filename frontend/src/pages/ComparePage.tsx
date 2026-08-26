import { useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useVehicleCompare } from '../api/queries'
import type { VehicleSearchResult } from '../api/types'
import { formatYearLabel } from '../lib/year'

const MAX_COMPARE_ITEMS = 4

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const GRID_COLS_BY_COUNT: Record<number, string> = {
  2: 'sm:grid-cols-2',
  3: 'sm:grid-cols-2 lg:grid-cols-3',
  4: 'sm:grid-cols-2 lg:grid-cols-4',
}

function parseIds(param: string | null): number[] {
  if (!param) {
    return []
  }
  const ids = param
    .split(',')
    .map((part) => Number(part.trim()))
    .filter((n) => Number.isInteger(n) && n > 0)
  return [...new Set(ids)]
}

export function ComparePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const idsParam = searchParams.get('ids')
  const ids = parseIds(idsParam)

  // Um link com mais de 4 ids (ex.: editado à mão) é truncado e a URL corrigida,
  // em vez de deixar o backend rejeitar com 400.
  useEffect(() => {
    const currentIds = parseIds(idsParam)
    if (currentIds.length > MAX_COMPARE_ITEMS) {
      setSearchParams({ ids: currentIds.slice(0, MAX_COMPARE_ITEMS).join(',') }, { replace: true })
    }
  }, [idsParam, setSearchParams])

  const effectiveIds = ids.slice(0, MAX_COMPARE_ITEMS)

  function handleRemove(id: number) {
    const remaining = effectiveIds.filter((existingId) => existingId !== id)
    if (remaining.length === 0) {
      setSearchParams({})
    } else {
      setSearchParams({ ids: remaining.join(',') })
    }
  }

  return (
    <div className="space-y-6">
      <Link to="/" className="text-sm text-blue-600 underline">
        ← Voltar para a busca
      </Link>

      <h1 className="text-2xl font-semibold text-slate-900">Comparador</h1>

      {effectiveIds.length === 0 ? (
        <EmptyState message="Nenhum veículo selecionado para comparar. Volte para a busca e escolha de 2 a 4 veículos." />
      ) : effectiveIds.length === 1 ? (
        <EmptyState message="Selecione pelo menos 2 veículos para comparar." />
      ) : (
        <CompareResults ids={effectiveIds} onRemove={handleRemove} />
      )}
    </div>
  )
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-6 text-center">
      <p className="text-sm text-slate-600">{message}</p>
      <Link to="/" className="mt-3 inline-block text-sm text-blue-600 underline">
        Ir para a busca
      </Link>
    </div>
  )
}

function CompareResults({ ids, onRemove }: { ids: number[]; onRemove: (id: number) => void }) {
  const query = useVehicleCompare(ids)

  if (query.isPending) {
    return <p className="text-sm text-slate-500">Carregando...</p>
  }

  if (query.isError) {
    return <p className="text-sm text-red-600">Erro ao carregar comparação: {query.error.message}</p>
  }

  const items = query.data

  if (items.length === 1) {
    return (
      <EmptyState message="Só foi possível encontrar 1 desses veículos (os demais podem ter sido removidos da base). Selecione pelo menos 2 para comparar." />
    )
  }

  if (items.length === 0) {
    return (
      <EmptyState message="Nenhum desses veículos foi encontrado. Volte para a busca e selecione novamente." />
    )
  }

  const prices = items.map((item) => item.price)
  const minPrice = Math.min(...prices)
  const maxPrice = Math.max(...prices)
  const hasPriceSpread = minPrice !== maxPrice

  return (
    <div className={`grid grid-cols-1 gap-4 ${GRID_COLS_BY_COUNT[items.length] ?? ''}`}>
      {items.map((item) => (
        <CompareCard
          key={item.id}
          item={item}
          isCheapest={hasPriceSpread && item.price === minPrice}
          isMostExpensive={hasPriceSpread && item.price === maxPrice}
          onRemove={() => onRemove(item.id)}
        />
      ))}
    </div>
  )
}

function CompareCard({
  item,
  isCheapest,
  isMostExpensive,
  onRemove,
}: {
  item: VehicleSearchResult
  isCheapest: boolean
  isMostExpensive: boolean
  onRemove: () => void
}) {
  const highlightClass = isCheapest
    ? 'border-green-400 bg-green-50'
    : isMostExpensive
      ? 'border-amber-400 bg-amber-50'
      : 'border-slate-200 bg-white'

  return (
    <div className={`relative rounded-lg border p-4 ${highlightClass}`}>
      <button
        type="button"
        onClick={onRemove}
        aria-label={`Remover ${item.brand} ${item.model} da comparação`}
        className="absolute right-2 top-2 text-slate-400 hover:text-slate-600"
      >
        ✕
      </button>

      <div className="flex flex-wrap gap-1 pr-6">
        {isCheapest && <Badge tone="green">Mais barato</Badge>}
        {isMostExpensive && <Badge tone="amber">Mais caro</Badge>}
      </div>

      <h2 className="mt-2 text-lg font-semibold text-slate-900">
        <Link to={`/vehicles/${item.modelId}`} className="underline">
          {item.brand} {item.model}
        </Link>
      </h2>

      <dl className="mt-3 space-y-1 text-sm text-slate-600">
        <div className="flex justify-between">
          <dt>Ano</dt>
          <dd>{formatYearLabel(Number(item.year))}</dd>
        </div>
        <div className="flex justify-between">
          <dt>Combustível</dt>
          <dd>{item.fuel}</dd>
        </div>
        <div className="flex justify-between">
          <dt>Código FIPE</dt>
          <dd>{item.fipeCode}</dd>
        </div>
      </dl>

      <p className="mt-3 text-xl font-bold text-slate-900">{currencyFormatter.format(item.price)}</p>
    </div>
  )
}

function Badge({ tone, children }: { tone: 'green' | 'amber'; children: string }) {
  const toneClass = tone === 'green' ? 'bg-green-600' : 'bg-amber-600'
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium text-white ${toneClass}`}>
      {children}
    </span>
  )
}
