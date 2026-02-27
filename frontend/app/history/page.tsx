"use client";

import { Header } from "@/components/layout/Header";

export default function HistoryPage() {
  return (
    <div className="min-h-screen">
      <Header
        title="Historical Context"
        subtitle="Transmission of the New Testament text"
      />

      <div className="p-6 space-y-8 max-w-3xl">
        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">Origem dos Papiros</h2>
          <p className="text-muted-foreground leading-relaxed">
            Os papiros do Novo Testamento provêm principalmente do Egito, onde o clima seco
            preservou materiais orgânicos por séculos. Destacam-se as descobertas em
            Oxyrhynchus (1897–1907), que revelaram milhares de fragmentos; os Papiros
            Chester Beatty (década de 1930), com códices quase completos de Paulo e
            evangelhos; e os Papiros Bodmer (década de 1950), incluindo P66 e P75 com
            textos joaninos e evangélicos de grande antiguidade.
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">Desenvolvimento do Códice</h2>
          <p className="text-muted-foreground leading-relaxed">
            A transição do rolo (volumen) para o códice (livro com páginas) foi decisiva
            para a transmissão do texto. O códice permitiu reunir múltiplos livros em um
            único volume, facilitou a consulta e a comparação de passagens e tornou-se o
            formato padrão para escritos cristãos já no século II. P52 (c. 125–175), o
            fragmento mais antigo do NT, já está em formato de códice.
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">Grandes Códices do Século IV</h2>
          <p className="text-muted-foreground leading-relaxed">
            O século IV produziu os grandes manuscritos unciais que contêm o NT completo
            ou quase completo: o Codex Sinaiticus (א, 01), descoberto no Mosteiro de
            Santa Catarina; o Codex Vaticanus (B, 03), conservado na Biblioteca Apostólica
            Vaticana; e o Codex Alexandrinus (A, 02), de proveniência alexandrina. Esses
            testemunhos são fundamentais para a crítica textual e a reconstrução do
            texto original.
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">Expansão Textual até o Século X</h2>
          <p className="text-muted-foreground leading-relaxed">
            A partir do século V, a escrita minúscula (cursiva) substituiu gradualmente
            a uncial nos manuscritos. Scriptoria em mosteiros bizantinos, como os de
            Constantinopla e do Monte Atos, copiaram e preservaram o texto em grande
            escala. A maioria dos mais de 5.800 manuscritos gregos do NT são minusculos
            dos séculos IX–XVI, testemunhando a expansão e estabilização da tradição
            textual bizantina.
          </p>
        </section>
      </div>
    </div>
  );
}
