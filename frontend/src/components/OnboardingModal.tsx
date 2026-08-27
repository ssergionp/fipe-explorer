import { useEffect, useRef } from 'react'

const FEATURES = [
  {
    title: 'Busca',
    description: 'Filtros combináveis (marca, modelo, ano, combustível) sobre a Tabela FIPE inteira.',
  },
  {
    title: 'Comparador',
    description: 'Selecione até 4 veículos na busca e compare lado a lado, com o mais barato destacado.',
  },
  {
    title: 'Alertas de preço',
    description: 'Crie uma conta e observe um veículo pra receber um e-mail quando o preço mudar.',
  },
]

export function OnboardingModal({ onDismiss }: { onDismiss: () => void }) {
  const dismissButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    dismissButtonRef.current?.focus()

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onDismiss()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onDismiss])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="onboarding-title"
        className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl"
      >
        <h2 id="onboarding-title" className="text-lg font-semibold text-slate-900">
          Bem-vindo ao FIPE Explorer
        </h2>

        <ul className="mt-4 space-y-4">
          {FEATURES.map((feature) => (
            <li key={feature.title}>
              <p className="text-sm font-medium text-slate-900">{feature.title}</p>
              <p className="mt-0.5 text-sm text-slate-600">{feature.description}</p>
            </li>
          ))}
        </ul>

        <button
          ref={dismissButtonRef}
          type="button"
          onClick={onDismiss}
          className="mt-6 w-full rounded-md bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-700"
        >
          Entendi
        </button>
      </div>
    </div>
  )
}
