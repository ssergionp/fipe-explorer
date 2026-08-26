export function PrivacyPolicyPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">Política de privacidade</h1>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Quais dados guardamos</h2>
        <p className="text-sm text-slate-700">
          Ao criar uma conta no FIPE Explorer, guardamos apenas o seu e-mail e uma versão
          criptografada (hash) da sua senha — nunca a senha em texto puro. Não pedimos nome,
          telefone, CPF ou qualquer outro dado pessoal nesta etapa.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Para que usamos</h2>
        <p className="text-sm text-slate-700">
          Só para autenticar o seu acesso à conta (login). Hoje a conta ainda não desbloqueia
          nenhuma funcionalidade nova — busca, comparador, insights e a calculadora de valor
          ajustado continuam totalmente públicos, sem exigir login. A conta é a base para recursos
          futuros que vão depender de saber "de quem" é o dado, como favoritos e alertas de preço.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Compartilhamento com terceiros</h2>
        <p className="text-sm text-slate-700">
          Não compartilhamos, vendemos ou repassamos seu e-mail a nenhum terceiro.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Exclusão de conta</h2>
        <p className="text-sm text-slate-700">
          Ainda não existe uma tela de autoatendimento para exclusão de conta e dados. Se quiser
          remover sua conta, entre em contato pelos canais informados no projeto.
        </p>
      </section>
    </div>
  )
}
