import { useParams } from 'react-router-dom'

export function VehicleDetailPage() {
  const { modelId } = useParams()

  return (
    <div>
      <h1 className="text-2xl font-semibold text-slate-900">Detalhe do veículo</h1>
      <p className="mt-2 text-sm text-slate-600">
        Em construção. Vai consumir <code>/api/v1/models/{modelId}/prices</code> para montar a
        curva de depreciação do modelo (id {modelId}).
      </p>
    </div>
  )
}
