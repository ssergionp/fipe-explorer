import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  error: Error | null
}

/**
 * Rede de segurança pra um erro inesperado de render (ex.: resposta da API em formato
 * diferente do previsto) não derrubar a aplicação inteira numa tela branca. Erros de
 * requisição (fetch, HTTP) já são tratados por página via TanStack Query — isso aqui cobre
 * exceções síncronas durante o render, que o React não recupera sozinho.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Erro não tratado na interface:', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center gap-3 px-4 text-center">
          <h1 className="text-lg font-semibold text-slate-900">Algo deu errado</h1>
          <p className="text-sm text-slate-600">
            Ocorreu um erro inesperado nesta tela. Tente recarregar a página.
          </p>
          <button
            type="button"
            onClick={() => {
              this.setState({ error: null })
              window.location.href = '/'
            }}
            className="rounded-md bg-slate-900 px-3 py-1.5 text-sm font-medium text-white"
          >
            Voltar para o início
          </button>
        </div>
      )
    }

    return this.props.children
  }
}
