import { createContext, useContext, useState, type ReactNode } from 'react'
import { apiPost } from '../api/client'
import type { AuthResponse } from '../api/types'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './tokenStorage'

interface AuthContextValue {
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, acceptedPrivacyPolicy: boolean) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

/**
 * Só guarda tokens/isAuthenticated — nenhum objeto de usuário aqui (mesmo formato do
 * task-manager-frontend). Quem precisa exibir o e-mail usa useCurrentUser(), que busca
 * /auth/me sob demanda em vez de decodificar o JWT no cliente.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getAccessToken())
  const [refreshToken, setRefreshToken] = useState<string | null>(() => getRefreshToken())

  function applyTokens(tokens: AuthResponse) {
    setTokens(tokens.token, tokens.refreshToken)
    setToken(tokens.token)
    setRefreshToken(tokens.refreshToken)
  }

  async function login(email: string, password: string) {
    const tokens = await apiPost<AuthResponse>('/auth/login', { email, password })
    applyTokens(tokens)
  }

  async function register(email: string, password: string, acceptedPrivacyPolicy: boolean) {
    const tokens = await apiPost<AuthResponse>('/auth/register', { email, password, acceptedPrivacyPolicy })
    applyTokens(tokens)
  }

  async function logout() {
    const currentRefreshToken = refreshToken
    clearTokens()
    setToken(null)
    setRefreshToken(null)

    if (currentRefreshToken) {
      try {
        await apiPost('/auth/logout', { refreshToken: currentRefreshToken })
      } catch {
        // best-effort: o estado local já foi limpo independente do backend responder.
      }
    }
  }

  return (
    <AuthContext.Provider value={{ isAuthenticated: !!token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth precisa ser usado dentro de um AuthProvider')
  }
  return context
}
