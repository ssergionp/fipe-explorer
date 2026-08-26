import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiGet } from './client'
import type {
  Brand,
  FuelType,
  ModelPriceHistory,
  PageResponse,
  SortBy,
  SortDir,
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

export function useModelPriceHistory(modelId: number | undefined) {
  return useQuery({
    queryKey: ['model-price-history', modelId],
    queryFn: () => apiGet<ModelPriceHistory>(`/models/${modelId}/prices`),
    enabled: modelId !== undefined,
    retry: false,
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
