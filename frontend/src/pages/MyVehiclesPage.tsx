import { Link } from 'react-router-dom'
import {
  useDeleteSavedPriceEstimate,
  useFavorites,
  useRemoveFavorite,
  useSavedPriceEstimates,
  useVehicleConditions,
  useVehicleExtras,
} from '../api/queries'
import type { LabeledValue } from '../api/types'
import { useAuth } from '../auth/AuthContext'

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const kmFormatter = new Intl.NumberFormat('pt-BR')

function labelFor(options: LabeledValue[] | undefined, key: string): string {
  return options?.find((option) => option.key === key)?.label ?? key
}

export function MyVehiclesPage() {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return (
      <div className="space-y-3">
        <h1 className="text-2xl font-semibold text-slate-900">Meus veículos</h1>
        <p className="text-sm text-slate-600">
          Você precisa{' '}
          <Link to="/login" className="text-blue-600 underline">
            entrar
          </Link>{' '}
          para ver seus favoritos e estimativas salvas.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-semibold text-slate-900">Meus veículos</h1>
      <FavoritesSection />
      <SavedEstimatesSection />
    </div>
  )
}

function FavoritesSection() {
  const favoritesQuery = useFavorites()
  const removeFavorite = useRemoveFavorite()

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold text-slate-900">Favoritos</h2>

      {favoritesQuery.isPending ? (
        <p className="text-sm text-slate-500">Carregando...</p>
      ) : favoritesQuery.isError ? (
        <p className="text-sm text-red-600">Erro ao carregar favoritos: {favoritesQuery.error.message}</p>
      ) : favoritesQuery.data.length === 0 ? (
        <p className="text-sm text-slate-500">Nenhum veículo favoritado ainda.</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-600">
              <tr>
                <th className="px-4 py-2 font-medium">Marca</th>
                <th className="px-4 py-2 font-medium">Modelo</th>
                <th className="px-4 py-2 font-medium">Ano</th>
                <th className="px-4 py-2 font-medium">Combustível</th>
                <th className="px-4 py-2 text-right font-medium">Preço</th>
                <th className="px-4 py-2 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {favoritesQuery.data.map((item) => (
                <tr key={item.id}>
                  <td className="px-4 py-2">{item.brand}</td>
                  <td className="px-4 py-2">
                    <Link to={`/vehicles/${item.modelId}`} className="text-blue-600 underline">
                      {item.model}
                    </Link>
                  </td>
                  <td className="px-4 py-2">{item.year}</td>
                  <td className="px-4 py-2">{item.fuel}</td>
                  <td className="px-4 py-2 text-right">{currencyFormatter.format(item.price)}</td>
                  <td className="px-4 py-2 text-right">
                    <button
                      type="button"
                      onClick={() => removeFavorite.mutate(item.id)}
                      disabled={removeFavorite.isPending}
                      className="rounded-md border border-slate-300 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function SavedEstimatesSection() {
  const savedEstimatesQuery = useSavedPriceEstimates()
  const deleteSavedEstimate = useDeleteSavedPriceEstimate()
  const conditionsQuery = useVehicleConditions()
  const extrasQuery = useVehicleExtras()

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold text-slate-900">Estimativas salvas</h2>

      {savedEstimatesQuery.isPending ? (
        <p className="text-sm text-slate-500">Carregando...</p>
      ) : savedEstimatesQuery.isError ? (
        <p className="text-sm text-red-600">Erro ao carregar estimativas: {savedEstimatesQuery.error.message}</p>
      ) : savedEstimatesQuery.data.length === 0 ? (
        <p className="text-sm text-slate-500">Nenhuma estimativa salva ainda.</p>
      ) : (
        <div className="space-y-3">
          {savedEstimatesQuery.data.map((estimate) => {
            const extrasLabel = estimate.extras.map((key) => labelFor(extrasQuery.data, key)).join(', ')
            return (
              <div key={estimate.id} className="rounded-lg border border-slate-200 bg-white p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <Link
                      to={`/vehicles/${estimate.vehicle.modelId}`}
                      className="font-medium text-blue-600 underline"
                    >
                      {estimate.vehicle.brand} {estimate.vehicle.model}
                    </Link>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {estimate.vehicle.year} · {estimate.vehicle.fuel} · {kmFormatter.format(estimate.km)} km ·{' '}
                      {labelFor(conditionsQuery.data, estimate.condition)}
                      {extrasLabel ? ` · ${extrasLabel}` : ''}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => deleteSavedEstimate.mutate(estimate.id)}
                    disabled={deleteSavedEstimate.isPending}
                    className="rounded-md border border-slate-300 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                  >
                    Remover
                  </button>
                </div>
                <div className="mt-3 flex gap-3">
                  <span className="text-xs text-slate-500">
                    Preço FIPE: {currencyFormatter.format(estimate.basePrice)}
                  </span>
                  <span className="text-sm font-semibold text-slate-900">
                    Estimado: {currencyFormatter.format(estimate.adjustedPrice)}
                  </span>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}
