export type VehicleType = 'CAR' | 'MOTORCYCLE' | 'TRUCK'

export interface Brand {
  id: number
  name: string
}

export interface VehicleModelSummary {
  id: number
  name: string
  vehicleType: string
}

export interface FuelType {
  id: number
  code: string
  name: string
}

export interface VehicleSearchResult {
  id: number
  modelId: number
  brand: string
  model: string
  year: string
  fuel: string
  price: number
  fipeCode: string
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type SortBy = 'MODEL_NAME' | 'PRICE'
export type SortDir = 'ASC' | 'DESC'

export interface PricePoint {
  priceEntryId: number
  yearCode: string
  yearValue: string
  fuel: string
  price: number
}

export interface ModelPriceHistory {
  modelId: number
  brand: string
  model: string
  vehicleType: VehicleType
  fipeCode: string
  prices: PricePoint[]
}

export type CalendarHistoryStatus = 'AVAILABLE' | 'NOT_FOUND' | 'RATE_LIMITED' | 'UNAVAILABLE'

export interface CalendarHistoryPoint {
  month: string
  referenceCode: string
  price: number
}

export interface CalendarHistoryResponse {
  status: CalendarHistoryStatus
  reason: string | null
  cached: boolean
  months: CalendarHistoryPoint[]
}

export interface StatsSummary {
  totalPriceEntries: number
  distinctModels: number
  minPrice: number
  avgPrice: number
  maxPrice: number
}

export interface TopBrand {
  brandId: number
  brandName: string
  avgPrice: number
  modelCount: number
}

export interface FuelDistributionEntry {
  fuel: string
  count: number
}

export type VehicleCondition = 'EXCELENTE' | 'BOM' | 'REGULAR' | 'RUIM'

export interface LabeledValue {
  key: string
  label: string
}

export interface PriceEstimateRequest {
  km: number
  condition: VehicleCondition
  extras: string[]
}

export interface PriceAdjustmentComponent {
  key: string
  label: string
  percent: number
  amount: number
}

export interface PriceEstimateResponse {
  basePrice: number
  adjustedPrice: number
  components: PriceAdjustmentComponent[]
}
