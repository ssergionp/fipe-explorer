export function TermsOfUsePage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">Termos de uso</h1>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">O que é o FIPE Explorer</h2>
        <p className="text-sm text-slate-700">
          Ferramenta de exploração e análise de dados públicos da Tabela FIPE (preços de veículos
          no Brasil) — busca, comparação, estatísticas e ferramentas de apoio à decisão de compra
          e venda. Não é o site oficial da FIPE nem substitui a consulta oficial.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Estimativa de valor ajustado</h2>
        <p className="text-sm text-slate-700">
          A calculadora de valor ajustado (quilometragem, estado de conservação e opcionais) é uma
          estimativa automática, baseada em regras simples definidas por este produto — não é uma
          avaliação profissional. Use como referência, não como garantia de valor de venda ou
          compra. Decisões de compra, venda ou financiamento são de responsabilidade exclusiva de
          quem as toma.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Alertas de preço</h2>
        <p className="text-sm text-slate-700">
          Observar um veículo é uma conveniência informativa: um alerta só é avaliado quando um
          novo conjunto de dados da Tabela FIPE é importado (mensalmente, na maioria dos casos) e
          depende do envio de e-mail funcionar corretamente. Não garantimos que todo alerta será
          disparado ou entregue — não use esse recurso como única forma de acompanhar o preço de
          um veículo em decisões urgentes.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Precisão dos dados</h2>
        <p className="text-sm text-slate-700">
          Os preços exibidos vêm de CSVs publicados pela própria Tabela FIPE, importados
          periodicamente. Podem haver atrasos entre uma atualização da Tabela FIPE e sua
          disponibilidade aqui, e eventuais erros de importação. Não nos responsabilizamos por
          decisões tomadas exclusivamente com base nos dados exibidos.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Sua conta</h2>
        <p className="text-sm text-slate-700">
          Você é responsável por manter sua senha em sigilo e por qualquer atividade realizada
          através da sua conta.
        </p>
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">Alterações nestes termos</h2>
        <p className="text-sm text-slate-700">
          Estes termos podem ser atualizados conforme o produto evolui. O uso continuado do FIPE
          Explorer após uma mudança implica concordância com a versão vigente.
        </p>
      </section>
    </div>
  )
}
