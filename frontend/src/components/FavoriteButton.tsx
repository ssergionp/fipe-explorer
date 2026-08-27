import { useNavigate } from 'react-router-dom'
import { useAddFavorite, useFavorites, useRemoveFavorite } from '../api/queries'
import { useAuth } from '../auth/AuthContext'

/**
 * Deslogado: clique manda pro login em vez de dar erro silencioso (o endpoint é protegido).
 * O estado de "favoritado" vem da lista completa de favoritos (cache do TanStack Query é
 * compartilhado entre todos os botões da página, então isso não dispara uma chamada por linha).
 */
export function FavoriteButton({ priceEntryId }: { priceEntryId: number }) {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const favoritesQuery = useFavorites()
  const addFavorite = useAddFavorite()
  const removeFavorite = useRemoveFavorite()

  const isFavorited = favoritesQuery.data?.some((favorite) => favorite.id === priceEntryId) ?? false
  const isPending = addFavorite.isPending || removeFavorite.isPending

  function handleClick() {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    if (isFavorited) {
      removeFavorite.mutate(priceEntryId)
    } else {
      addFavorite.mutate(priceEntryId)
    }
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={isPending}
      aria-label={isFavorited ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
      title={isFavorited ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
      className={`text-lg leading-none disabled:opacity-50 ${
        isFavorited ? 'text-accent-500 hover:text-accent-600' : 'text-slate-300 hover:text-accent-500'
      }`}
    >
      {isFavorited ? '★' : '☆'}
    </button>
  )
}
