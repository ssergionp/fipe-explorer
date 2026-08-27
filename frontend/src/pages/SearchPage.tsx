import type { ReactNode } from 'react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  useBrands,
  useFuelTypes,
  useModels,
  useVehicleSearch,
  useVehicleTypes,
  useYears,
  type VehicleSearchFilters,
} from '../api/queries'
import type { SortBy, SortDir, VehicleType } from '../api/types'
import { FavoriteButton } from '../components/FavoriteButton'

const PAGE_SIZE = 20
const MAX_COMPARE_ITEMS = 4

const VEHICLE_TYPE_LABELS: Record<VehicleType, string> = {
  CAR: 'Carro',
  MOTORCYCLE: 'Moto',
  TRUCK: 'Caminhão',
}

const ZERO_KM_YEAR_CODE = 32000

const SORT_OPTIONS: { label: string; sortBy: SortBy; sortDir: SortDir }[] = [
  { label: 'Modelo (A-Z)', sortBy: 'MODEL_NAME', sortDir: 'ASC' },
  { label: 'Modelo (Z-A)', sortBy: 'MODEL_NAME', sortDir: 'DESC' },
  { label: 'Preço (menor primeiro)', sortBy: 'PRICE', sortDir: 'ASC' },
  { label: 'Preço (maior primeiro)', sortBy: 'PRICE', sortDir: 'DESC' },
]

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const selectClass =
  'rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm text-slate-900 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400'

function formatYear(year: number) {
  return year === ZERO_KM_YEAR_CODE ? 'Zero KM' : String(year)
}

export function SearchPage() {
  const navigate = useNavigate()
  const [type, setType] = useState<VehicleType | undefined>(undefined)
  const [brandId, setBrandId] = useState<number | undefined>(undefined)
  const [modelId, setModelId] = useState<number | undefined>(undefined)
  const [year, setYear] = useState<number | undefined>(undefined)
  const [fuel, setFuel] = useState<string | undefined>(undefined)
  const [sortIndex, setSortIndex] = useState(0)
  const [page, setPage] = useState(0)
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  const vehicleTypesQuery = useVehicleTypes()
  const brandsQuery = useBrands(type)
  const modelsQuery = useModels(brandId, type)
  const yearsQuery = useYears(type)
  const fuelTypesQuery = useFuelTypes()

  const sort = SORT_OPTIONS[sortIndex]
  const filters: VehicleSearchFilters = {
    type,
    brandId,
    modelId,
    year,
    fuel,
    page,
    size: PAGE_SIZE,
    sortBy: sort.sortBy,
    sortDir: sort.sortDir,
  }
  const searchQuery = useVehicleSearch(filters)

  function handleTypeChange(value: string) {
    setType(value === '' ? undefined : (value as VehicleType))
    setBrandId(undefined)
    setModelId(undefined)
    setYear(undefined)
    setPage(0)
  }

  function handleBrandChange(value: string) {
    setBrandId(value === '' ? undefined : Number(value))
    setModelId(undefined)
    setPage(0)
  }

  function handleModelChange(value: string) {
    setModelId(value === '' ? undefined : Number(value))
    setPage(0)
  }

  function handleYearChange(value: string) {
    setYear(value === '' ? undefined : Number(value))
    setPage(0)
  }

  function handleFuelChange(value: string) {
    setFuel(value === '' ? undefined : value)
    setPage(0)
  }

  function handleSortChange(value: string) {
    setSortIndex(Number(value))
    setPage(0)
  }

  function toggleSelected(id: number) {
    setSelectedIds((prev) => {
      if (prev.includes(id)) {
        return prev.filter((selectedId) => selectedId !== id)
      }
      if (prev.length >= MAX_COMPARE_ITEMS) {
        return prev
      }
      return [...prev, id]
    })
  }

  function handleCompare() {
    navigate(`/compare?ids=${selectedIds.join(',')}`)
  }

  const filterErrorLabels: [boolean, string][] = [
    [vehicleTypesQuery.isError, 'tipos de veículo'],
    [brandsQuery.isError, 'marcas'],
    [modelsQuery.isError, 'modelos'],
    [yearsQuery.isError, 'anos'],
    [fuelTypesQuery.isError, 'combustíveis'],
  ]
  const failedFilters = filterErrorLabels.filter(([hasError]) => hasError).map(([, label]) => label)

  return (
    <div className={`space-y-6 ${selectedIds.length > 0 ? 'pb-16' : ''}`}>
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Busca</h1>
        <p className="mt-1 text-sm text-slate-600">
          Selecione um tipo de veículo para começar. Marca, modelo, ano e combustível são
          opcionais e combináveis entre si.
        </p>
      </div>

      {failedFilters.length > 0 && (
        <p className="text-sm text-red-600" role="alert">
          Não foi possível carregar as opções de: {failedFilters.join(', ')}. Verifique sua
          conexão e recarregue a página.
        </p>
      )}

      <div className="grid grid-cols-2 gap-4 rounded-lg border border-slate-200 bg-white p-4 sm:grid-cols-3 lg:grid-cols-6">
        <Field label="Tipo">
          <select
            className={selectClass}
            value={type ?? ''}
            onChange={(e) => handleTypeChange(e.target.value)}
          >
            <option value="">Selecione</option>
            {vehicleTypesQuery.data?.map((vt) => (
              <option key={vt} value={vt}>
                {VEHICLE_TYPE_LABELS[vt] ?? vt}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Marca">
          <select
            className={selectClass}
            value={brandId ?? ''}
            onChange={(e) => handleBrandChange(e.target.value)}
            disabled={type === undefined}
          >
            <option value="">Todas</option>
            {brandsQuery.data?.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Modelo">
          <select
            className={selectClass}
            value={modelId ?? ''}
            onChange={(e) => handleModelChange(e.target.value)}
            disabled={brandId === undefined}
          >
            <option value="">Todos</option>
            {modelsQuery.data?.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Ano">
          <select
            className={selectClass}
            value={year ?? ''}
            onChange={(e) => handleYearChange(e.target.value)}
            disabled={type === undefined}
          >
            <option value="">Todos</option>
            {yearsQuery.data?.map((y) => (
              <option key={y} value={y}>
                {formatYear(y)}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Combustível">
          <select
            className={selectClass}
            value={fuel ?? ''}
            onChange={(e) => handleFuelChange(e.target.value)}
            disabled={type === undefined}
          >
            <option value="">Todos</option>
            {fuelTypesQuery.data?.map((f) => (
              <option key={f.id} value={f.name}>
                {f.name}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Ordenar por">
          <select
            className={selectClass}
            value={sortIndex}
            onChange={(e) => handleSortChange(e.target.value)}
          >
            {SORT_OPTIONS.map((option, index) => (
              <option key={option.label} value={index}>
                {option.label}
              </option>
            ))}
          </select>
        </Field>
      </div>

      {type === undefined ? (
        <p className="text-sm text-slate-500">
          Selecione um tipo de veículo para ver os resultados.
        </p>
      ) : (
        <ResultsSection
          query={searchQuery}
          page={page}
          onPageChange={setPage}
          selectedIds={selectedIds}
          onToggleSelected={toggleSelected}
        />
      )}

      {selectedIds.length > 0 && (
        <div className="fixed inset-x-0 bottom-0 border-t border-slate-200 bg-white px-4 py-3 shadow-lg">
          <div className="mx-auto flex max-w-5xl items-center justify-between">
            <span className="text-sm text-slate-700">
              {selectedIds.length} de {MAX_COMPARE_ITEMS} selecionados para comparação
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50"
                onClick={() => setSelectedIds([])}
              >
                Limpar seleção
              </button>
              <button
                type="button"
                className="rounded-md bg-slate-900 px-3 py-1.5 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-40"
                disabled={selectedIds.length < 2}
                onClick={handleCompare}
              >
                Comparar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-1 text-sm text-slate-700">
      <span className="font-medium">{label}</span>
      {children}
    </label>
  )
}

function ResultsSection({
  query,
  page,
  onPageChange,
  selectedIds,
  onToggleSelected,
}: {
  query: ReturnType<typeof useVehicleSearch>
  page: number
  onPageChange: (page: number) => void
  selectedIds: number[]
  onToggleSelected: (id: number) => void
}) {
  if (query.isPending) {
    return <p className="text-sm text-slate-500">Carregando resultados...</p>
  }

  if (query.isError) {
    return (
      <p className="text-sm text-red-600">
        Erro ao buscar veículos: {query.error.message}
      </p>
    )
  }

  const data = query.data

  if (data.items.length === 0) {
    return (
      <p className="text-sm text-slate-500">
        Nenhum veículo encontrado para os filtros selecionados.
      </p>
    )
  }

  return (
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              <th className="px-4 py-2 font-medium">
                <span className="sr-only">Comparar</span>
              </th>
              <th className="px-4 py-2 font-medium">
                <span className="sr-only">Favoritar</span>
              </th>
              <th className="px-4 py-2 font-medium">Marca</th>
              <th className="px-4 py-2 font-medium">Modelo</th>
              <th className="px-4 py-2 font-medium">Ano</th>
              <th className="px-4 py-2 font-medium">Combustível</th>
              <th className="px-4 py-2 font-medium">Código FIPE</th>
              <th className="px-4 py-2 text-right font-medium">Preço</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {data.items.map((item) => {
              const isSelected = selectedIds.includes(item.id)
              const isDisabled = !isSelected && selectedIds.length >= 4
              return (
                <tr key={item.id} className="hover:bg-slate-50">
                  <td className="px-4 py-2">
                    <input
                      type="checkbox"
                      aria-label={`Adicionar ${item.brand} ${item.model} (${item.fuel}, ${item.year}) à comparação`}
                      title={isDisabled ? 'Limite de 4 veículos na comparação atingido' : undefined}
                      checked={isSelected}
                      disabled={isDisabled}
                      onChange={() => onToggleSelected(item.id)}
                    />
                  </td>
                  <td className="px-4 py-2">
                    <FavoriteButton priceEntryId={item.id} />
                  </td>
                  <td className="px-4 py-2">{item.brand}</td>
                  <td className="px-4 py-2">
                    <Link to={`/vehicles/${item.modelId}`} className="text-brand-600 underline hover:text-brand-700">
                      {item.model}
                    </Link>
                  </td>
                  <td className="px-4 py-2">
                    {item.year === String(ZERO_KM_YEAR_CODE) ? 'Zero KM' : item.year}
                  </td>
                  <td className="px-4 py-2">{item.fuel}</td>
                  <td className="px-4 py-2">{item.fipeCode}</td>
                  <td className="px-4 py-2 text-right">{currencyFormatter.format(item.price)}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-sm text-slate-600">
        <span>
          Página {data.page + 1} de {Math.max(data.totalPages, 1)} — {data.totalElements}{' '}
          resultados
        </span>
        <div className="flex gap-2">
          <button
            type="button"
            className="rounded-md border border-slate-300 px-3 py-1 disabled:opacity-40"
            disabled={data.page === 0}
            onClick={() => onPageChange(page - 1)}
          >
            Anterior
          </button>
          <button
            type="button"
            className="rounded-md border border-slate-300 px-3 py-1 disabled:opacity-40"
            disabled={data.page + 1 >= data.totalPages}
            onClick={() => onPageChange(page + 1)}
          >
            Próxima
          </button>
        </div>
      </div>
    </div>
  )
}
