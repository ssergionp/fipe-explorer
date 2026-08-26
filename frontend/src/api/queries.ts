import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiGet } from './client'
import type {
  Brand,
  CalendarHistoryResponse,
  FuelDistributionEntry,
  FuelType,
  ModelPriceHistory,
  PageResponse,
  SortBy,
  SortDir,
  StatsSummary,
  TopBrand,
  VehicleModelSummary,
  VehicleSearchResult,
  VehicleType,
} from './types'

export function useVehicleTypes() {
  return useQuery({
    queryKey: ['vehicle-types'],
    queryFn: () => apiGet<VehicleType[]>('/vehicle-types'),
  })
}

export function useBrands(type: VehicleType | undefined) {
  return useQuery({
    queryKey: ['brands', type],
    queryFn: () => apiGet<Brand[]>('/brands', { type }),
    enabled: type !== undefined,
  })
}

export function useModels(brandId: number | undefined, type: VehicleType | undefined) {
  return useQuery({
    queryKey: ['models', brandId, type],
    queryFn: () => apiGet<VehicleModelSummary[]>(`/brands/${brandId}/models`, { type }),
    enabled: brandId !== undefined && type !== undefined,
  })
}

export function useYears(type: VehicleType | undefined) {
  return useQuery({
    queryKey: ['years', type],
    queryFn: () => apiGet<number[]>(`/vehicle-types/${type}/years`),
    enabled: type !== undefined,
  })
}

export function useFuelTypes() {
  return useQuery({
    queryKey: ['fuel-types'],
    queryFn: () => apiGet<FuelType[]>('/fuel-types'),
  })
}

export function useVehicleCompare(ids: number[]) {
  return useQuery({
    queryKey: ['vehicle-compare', ids],
    queryFn: () => apiGet<VehicleSearchResult[]>('/vehicles/compare', { ids: ids.join(',') }),
    enabled: ids.length >= 2 && ids.length <= 4,
  })
}

export function useModelPriceHistory(modelId: number | undefined) {
  return useQuery({
    queryKey: ['model-price-history', modelId],
    queryFn: () => apiGet<ModelPriceHistory>(`/models/${modelId}/prices`),
    enabled: modelId !== undefined,
    retry: false,
  })
}

/**
 * Só é chamado quando montado (a tela de Detalhe renderiza o painel sob demanda, ao clicar em
 * "Ver histórico real de preço" numa linha) — a API pública da FIPE tem cota diária baixa
 * (500 requisições sem token), então isso nunca deve disparar automaticamente para todas as
 * linhas da tabela.
 */
export function useCalendarHistory(type: VehicleType, fipeCode: string, yearCode: string) {
  return useQuery({
    queryKey: ['calendar-history', type, fipeCode, yearCode],
    queryFn: () => apiGet<CalendarHistoryResponse>('/vehicles/calendar-history', { type, fipeCode, yearCode }),
    retry: false,
  })
}

export function useStatsSummary(type: VehicleType | undefined) {
  return useQuery({
    queryKey: ['stats-summary', type],
    queryFn: () => apiGet<StatsSummary>('/stats/summary', { type }),
    enabled: type !== undefined,
  })
}

export function useTopBrands(type: VehicleType | undefined, order: 'asc' | 'desc', limit: number) {
  return useQuery({
    queryKey: ['stats-top-brands', type, order, limit],
    queryFn: () => apiGet<TopBrand[]>('/stats/top-brands', { type, order, limit }),
    enabled: type !== undefined,
  })
}

export function useFuelDistribution(type: VehicleType | undefined) {
  return useQuery({
    queryKey: ['stats-fuel-distribution', type],
    queryFn: () => apiGet<FuelDistributionEntry[]>('/stats/fuel-distribution', { type }),
    enabled: type !== undefined,
  })
}

export interface VehicleSearchFilters {
  type: VehicleType | undefined
  brandId: number | undefined
  modelId: number | undefined
  year: number | undefined
  fuel: string | undefined
  page: number
  size: number
  sortBy: SortBy
  sortDir: SortDir
}

export function useVehicleSearch(filters: VehicleSearchFilters) {
  return useQuery({
    queryKey: ['vehicle-search', filters],
    queryFn: () =>
      apiGet<PageResponse<VehicleSearchResult>>('/vehicles/search', {
        type: filters.type,
        brandId: filters.brandId,
        modelId: filters.modelId,
        year: filters.year,
        fuel: filters.fuel,
        page: filters.page,
        size: filters.size,
        sortBy: filters.sortBy,
        sortDir: filters.sortDir,
      }),
    enabled: filters.type !== undefined,
    placeholderData: keepPreviousData,
  })
}
