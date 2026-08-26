import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { useCurrentUser } from './auth/useCurrentUser'

function navLinkClass({ isActive }: { isActive: boolean }) {
  return `rounded-md px-3 py-2 text-sm font-medium ${
    isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
  }`
}

export function Layout() {
  const { isAuthenticated, logout } = useAuth()
  const currentUserQuery = useCurrentUser()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-5xl items-center gap-2 px-4 py-3">
          <span className="mr-4 text-lg font-semibold text-slate-900">FIPE Explorer</span>
          <NavLink to="/" end className={navLinkClass}>
            Busca
          </NavLink>
          <NavLink to="/compare" className={navLinkClass}>
            Comparador
          </NavLink>
          <NavLink to="/insights" className={navLinkClass}>
            Insights
          </NavLink>

          <div className="ml-auto flex items-center gap-3">
            {isAuthenticated ? (
              <>
                {currentUserQuery.data && (
                  <span className="text-sm text-slate-600">{currentUserQuery.data.email}</span>
                )}
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Sair
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className={navLinkClass}>
                  Entrar
                </NavLink>
                <NavLink to="/cadastro" className={navLinkClass}>
                  Cadastrar
                </NavLink>
              </>
            )}
          </div>
        </nav>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
