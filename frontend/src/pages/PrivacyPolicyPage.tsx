export function PrivacyPolicyPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">Política de privacidade</h1>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Quais dados guardamos</h2>
        <p className="text-sm text-slate-700">
          Ao criar uma conta no FIPE Explorer, guardamos o seu e-mail e uma versão criptografada
          (hash) da sua senha — nunca a senha em texto puro. Não pedimos nome, telefone, CPF ou
          qualquer outro dado pessoal no cadastro. Conforme você usa as funcionalidades que exigem
          login, também guardamos: veículos favoritados, estimativas de valor ajustado que você
          salvar (quilometragem, estado de conservação, opcionais e o resultado calculado) e
          veículos que você observar para alertas de preço.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Para que usamos</h2>
        <p className="text-sm text-slate-700">
          Pra autenticar o seu acesso à conta e pra viabilizar as funcionalidades que dependem de
          saber "de quem" é o dado: favoritos, estimativas salvas e alertas de preço. Busca,
          comparador, insights e a calculadora de valor ajustado continuam públicos, sem exigir
          login.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Alertas de preço por e-mail</h2>
        <p className="text-sm text-slate-700">
          Ao observar um veículo, guardamos o código FIPE dele, o percentual de variação que você
          configurou (ou o padrão de 5%) e a data em que você começou a observá-lo, vinculados à
          sua conta. Não é um e-mail periódico: só enviamos um alerta quando novos dados da Tabela
          FIPE são importados (normalmente uma vez por mês) <em>e</em> o preço de algum veículo que
          você observa variou acima do percentual configurado. Pra parar de receber, desmarque
          "observar" na ficha do veículo ou use "Parar de observar" na aba Alertas em "Meus
          veículos" — isso remove o registro e interrompe os e-mails imediatamente.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Compartilhamento com terceiros</h2>
        <p className="text-sm text-slate-700">
          Não vendemos nem repassamos seu e-mail para fins de marketing ou publicidade. O único
          compartilhamento que fazemos é com o provedor de envio de e-mail transacional (Resend),
          exclusivamente para entregar os alertas de preço que você configurou — nenhum outro dado
          seu é enviado a esse provedor.
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
