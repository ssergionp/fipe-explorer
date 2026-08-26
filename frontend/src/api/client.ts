import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '../auth/tokenStorage'
import type { AuthResponse } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

if (!API_BASE_URL) {
  throw new Error(
    'VITE_API_BASE_URL não configurada. Copie frontend/.env.example para frontend/.env.local.',
  )
}

export type QueryParams = Record<string, string | number | undefined>

// Renovação reativa (dispara no 401, não por timer) com fila single-flight: se várias chamadas
// tomam 401 ao mesmo tempo, todas esperam a MESMA renovação em andamento em vez de disparar uma
// pra cada. Mesmo padrão usado no task-manager-frontend, portado de axios (interceptor) pra fetch.
let refreshPromise: Promise<void> | null = null

function isAuthPath(url: string | URL): boolean {
  return url.toString().includes('/auth/')
}

async function refreshTokens(): Promise<void> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    clearTokens()
    throw new Error('Sem sessão para renovar')
  }

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  if (!response.ok) {
    clearTokens()
    throw new Error('Falha ao renovar a sessão')
  }

  const data = (await response.json()) as AuthResponse
  setTokens(data.token, data.refreshToken)
}

async function request<T>(url: string | URL, init: RequestInit, isRetry = false): Promise<T> {
  const token = getAccessToken()
  const headers = new Headers(init.headers)
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(url, { ...init, headers })

  if (response.status === 401 && !isRetry && !isAuthPath(url)) {
    if (!refreshPromise) {
      refreshPromise = refreshTokens().finally(() => {
        refreshPromise = null
      })
    }
    try {
      await refreshPromise
    } catch {
      window.location.href = '/login'
      throw new Error('Sessão expirada')
    }
    return request<T>(url, init, true)
  }

  if (!response.ok) {
    throw new Error(`Erro ${response.status} ao chamar ${url}`)
  }
  return response.json() as Promise<T>
}

export async function apiGet<T>(path: string, params: QueryParams = {}): Promise<T> {
  const url = new URL(`${API_BASE_URL}${path}`)
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  return request<T>(url, {})
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}
