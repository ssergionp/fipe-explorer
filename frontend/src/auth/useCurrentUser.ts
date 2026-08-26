import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../api/client'
import type { CurrentUser } from '../api/types'
import { useAuth } from './AuthContext'

/** Busca o e-mail do usuário logado no backend — não decodifica o JWT no cliente. */
export function useCurrentUser() {
  const { isAuthenticated } = useAuth()

  return useQuery({
    queryKey: ['current-user'],
    queryFn: () => apiGet<CurrentUser>('/auth/me'),
    enabled: isAuthenticated,
    retry: false,
  })
}
