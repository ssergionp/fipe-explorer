import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useUnwatchVehicle, useWatchVehicle, useWatchedVehicles } from '../api/queries'
import { useAuth } from '../auth/AuthContext'

const DEFAULT_THRESHOLD_PERCENT = 5

const percentFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'percent',
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

/**
 * Ao contrário do favorito (por linha de preço), observar é por veículo inteiro (fipeCode) — o
 * alerta compara todas as combinações de ano/combustível daquele veículo, não uma linha só.
 */
export function WatchVehicleButton({ fipeCode }: { fipeCode: string }) {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const watchedQuery = useWatchedVehicles()
  const watchVehicle = useWatchVehicle()
  const unwatchVehicle = useUnwatchVehicle()

  const [isFormOpen, setIsFormOpen] = useState(false)
  const [thresholdInput, setThresholdInput] = useState(String(DEFAULT_THRESHOLD_PERCENT))

  const watched = watchedQuery.data?.find((item) => item.fipeCode === fipeCode)
  const isPending = watchVehicle.isPending || unwatchVehicle.isPending

  function handleButtonClick() {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    if (watched) {
      unwatchVehicle.mutate(fipeCode)
      return
    }
    setIsFormOpen((open) => !open)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = thresholdInput.trim()
    const thresholdPercent = trimmed === '' ? undefined : Number(trimmed) / 100
    if (thresholdPercent !== undefined && (!Number.isFinite(thresholdPercent) || thresholdPercent <= 0)) {
      return
    }
    watchVehicle.mutate(
      { fipeCode, thresholdPercent },
      { onSuccess: () => setIsFormOpen(false) },
    )
  }

  return (
    <div>
      <button
        type="button"
        onClick={handleButtonClick}
        disabled={isPending}
        className="rounded-md border border-slate-300 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
      >
        {watched
          ? `🔕 Observando (${percentFormatter.format(watched.thresholdPercent)}) · Parar`
          : '🔔 Observar preço'}
      </button>

      {isFormOpen && !watched && (
        <form
          onSubmit={handleSubmit}
          className="mt-2 flex items-end gap-2 rounded-md border border-slate-200 bg-slate-50 p-2"
        >
          <label className="flex flex-col text-xs font-medium text-slate-600">
            Avisar quando o preço variar (%)
            <input
              type="number"
              min={0.01}
              step="any"
              value={thresholdInput}
              onChange={(event) => setThresholdInput(event.target.value)}
              placeholder={String(DEFAULT_THRESHOLD_PERCENT)}
              className="mt-1 w-20 rounded-md border border-slate-300 px-2 py-1 text-sm text-slate-900"
            />
          </label>
          <button
            type="submit"
            disabled={watchVehicle.isPending}
            className="rounded-md bg-slate-900 px-2 py-1 text-xs font-medium text-white hover:bg-slate-800 disabled:opacity-50"
          >
            Confirmar
          </button>
          <button
            type="button"
            onClick={() => setIsFormOpen(false)}
            className="rounded-md border border-slate-300 px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50"
          >
            Cancelar
          </button>
        </form>
      )}

      {watchVehicle.isError && <p className="mt-1 text-xs text-red-600">Erro ao observar: {watchVehicle.error.message}</p>}
    </div>
  )
}
