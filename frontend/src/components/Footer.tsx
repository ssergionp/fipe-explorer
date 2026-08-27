import { Link } from 'react-router-dom'

export function Footer() {
  return (
    <footer className="border-t border-slate-200 bg-white">
      <div className="mx-auto flex max-w-5xl flex-wrap gap-x-4 gap-y-1 px-4 py-4 text-xs text-slate-500">
        <span>FIPE Explorer</span>
        <Link to="/termos" className="hover:text-brand-600 hover:underline">
          Termos de uso
        </Link>
        <Link to="/privacidade" className="hover:text-brand-600 hover:underline">
          Política de privacidade
        </Link>
      </div>
    </footer>
  )
}
