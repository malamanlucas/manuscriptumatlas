"use client";

import { Header } from "@/components/layout/Header";

export default function SourcesPage() {
  return (
    <div className="min-h-screen">
      <Header
        title="Fontes de Dados"
        subtitle="Origem, metodologia e limitações dos dados"
      />

      <div className="p-6 space-y-8 max-w-3xl">
        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">
            Fonte Primária — NTVMR
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-3">
            Os dados manuscritológicos são obtidos do{" "}
            <strong className="text-foreground">
              New Testament Virtual Manuscript Room (NTVMR)
            </strong>
            , mantido pelo Institute for New Testament Textual Research (INTF) da
            Universidade de Münster, Alemanha.
          </p>
          <p className="text-muted-foreground leading-relaxed mb-3">
            A API do NTVMR expõe transcrições no formato TEI/XML. Para cada
            manuscrito, o sistema consulta o endpoint de transcrições com o
            parâmetro <code className="text-xs bg-secondary px-1 py-0.5 rounded">format=teiraw</code>,
            que retorna a marcação XML com elementos <code className="text-xs bg-secondary px-1 py-0.5 rounded">&lt;ab&gt;</code>{" "}
            indicando quais versículos estão presentes no manuscrito.
          </p>
          <p className="text-muted-foreground leading-relaxed">
            O fluxo de ingestão parseia o XML, extrai os versículos presentes e
            vincula cada um ao registro do manuscrito no banco de dados. Quando a
            API NTVMR não está disponível ou não retorna dados para um manuscrito
            específico, o sistema utiliza intervalos definidos em seed data como
            fallback.
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">
            Base Gregory-Aland
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-3">
            O sistema de catalogação Gregory-Aland atribui identificadores únicos
            a cada manuscrito grego do Novo Testamento. A numeração segue
            convenções por tipo:
          </p>
          <ul className="list-disc list-inside text-muted-foreground space-y-1 ml-2">
            <li>
              <strong className="text-foreground">Papiros:</strong> prefixo P + número (P1–P140+)
            </li>
            <li>
              <strong className="text-foreground">Unciais:</strong> numeração 01–0323+ (ou letras gregas/latinas para os mais antigos)
            </li>
            <li>
              <strong className="text-foreground">Minúsculos:</strong> numeração 1–2900+
            </li>
            <li>
              <strong className="text-foreground">Lecionários:</strong> prefixo l + número (l1–l2500+)
            </li>
          </ul>
          <p className="text-muted-foreground leading-relaxed mt-3">
            O catálogo é mantido pelo INTF e atualizado conforme novos manuscritos
            são descobertos ou reclassificados.
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">
            Metodologia de Datação
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-3">
            Para manuscritos com datação em intervalo (ex.: "século II/III"), o
            sistema adota uma abordagem <strong className="text-foreground">conservadora</strong>:
            utiliza o século mais antigo do intervalo (<code className="text-xs bg-secondary px-1 py-0.5 rounded">centuryMin</code>).
            Isto significa que se um manuscrito é datado entre os séculos II e
            III, ele é contabilizado como atestação do século II.
          </p>
          <p className="text-muted-foreground leading-relaxed">
            A <strong className="text-foreground">cobertura cumulativa</strong> para um dado
            século N inclui todos os manuscritos dos séculos 1 até N. Assim, a
            cobertura do século V conta com papiros do século II, unciais do
            século III, e assim por diante. Um versículo que aparece em múltiplos
            manuscritos é contado apenas uma vez (deduplicação).
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">
            Limitações do Projeto
          </h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>
              <strong className="text-foreground">Manuscritos fragmentários:</strong> Muitos papiros
              e unciais são fragmentos que preservam apenas porções de livros. O
              sistema registra com precisão quais versículos estão presentes, mas
              fragmentos menores contribuem menos para a cobertura total.
            </p>
            <p>
              <strong className="text-foreground">Datação paleográfica aproximada:</strong> A
              datação de manuscritos antigos é baseada em análise paleográfica
              (estilo de escrita), que tem margem de erro de 50–100 anos. As datas
              utilizadas são estimativas acadêmicas consensuais, não datas absolutas.
            </p>
            <p>
              <strong className="text-foreground">Não mede variantes textuais:</strong> Este
              sistema mede <em>cobertura</em> (se um versículo existe em pelo menos
              um manuscrito), não <em>variação textual</em> (diferenças entre
              manuscritos). A existência de um versículo em um manuscrito não
              implica que seu texto é idêntico ao de outros manuscritos.
            </p>
          </div>
        </section>
      </div>
    </div>
  );
}
