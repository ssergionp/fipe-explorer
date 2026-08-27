import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [acceptedPrivacyPolicy, setAcceptedPrivacyPolicy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    if (password !== confirmPassword) {
      setError('As senhas não coincidem.')
      return
    }

    setLoading(true)
    try {
      await register(email, password, acceptedPrivacyPolicy)
      navigate('/')
    } catch (err) {
      const message = err instanceof Error ? err.message : ''
      if (message.includes('409')) {
        setError('Este e-mail já está cadastrado.')
      } else if (message.includes('400')) {
        setError('Dados inválidos: a senha precisa ter pelo menos 8 caracteres, com letras e números.')
      } else {
        setError('Não foi possível criar a conta. Tente novamente.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">Criar conta</h1>

      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block text-sm font-medium text-slate-600">
          E-mail
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900"
          />
        </label>

        <label className="block text-sm font-medium text-slate-600">
          Senha
          <input
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900"
          />
          <span className="mt-1 block text-xs font-normal text-slate-500">
            Mínimo 8 caracteres, com letras e números.
          </span>
        </label>

        <label className="block text-sm font-medium text-slate-600">
          Confirmar senha
          <input
            type="password"
            required
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900"
          />
        </label>

        <label className="flex items-start gap-2 text-sm font-normal text-slate-700">
          <input
            type="checkbox"
            required
            checked={acceptedPrivacyPolicy}
            onChange={(event) => setAcceptedPrivacyPolicy(event.target.checked)}
            className="mt-0.5"
          />
          <span>
            Li e aceito a{' '}
            <Link to="/privacidade" className="text-brand-600 underline hover:text-brand-700" target="_blank">
              política de privacidade
            </Link>
            .
          </span>
        </label>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={loading || !acceptedPrivacyPolicy}
          className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
        >
          {loading ? 'Criando conta...' : 'Criar conta'}
        </button>
      </form>

      <p className="text-sm text-slate-600">
        Já tem conta?{' '}
        <Link to="/login" className="text-brand-600 underline hover:text-brand-700">
          Entrar
        </Link>
      </p>
    </div>
  )
}
