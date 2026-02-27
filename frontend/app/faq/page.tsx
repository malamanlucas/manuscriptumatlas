"use client";

import { useState } from "react";
import Link from "next/link";
import { Header } from "@/components/layout/Header";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface FaqItem {
  question: string;
  answer: React.ReactNode;
}

interface FaqCategory {
  title: string;
  items: FaqItem[];
}

const FAQ_DATA: FaqCategory[] = [
  {
    title: "Perguntas comuns de um cético",
    items: [
      {
        question: "O Novo Testamento foi alterado ao longo dos séculos?",
        answer: (
          <>
            A tradição manuscrita do NT é extraordinariamente rica. Com mais de
            5.800 manuscritos gregos, é possível reconstruir o texto original com
            alto grau de confiança. A maioria das variantes textuais são erros de
            cópia triviais (ortografia, ordem de palavras) que não afetam o
            significado. As variantes significativas representam menos de 1% do
            texto e nenhuma doutrina cristã central depende de passagens disputadas.
          </>
        ),
      },
      {
        question:
          "Quantos versículos não aparecem nos manuscritos mais antigos?",
        answer: (
          <>
            Nos manuscritos até o século III (papiros), a cobertura do NT já
            ultrapassa 40% dos versículos. Até o século V, com os grandes códices
            unciais, a cobertura supera 90%. Os versículos &quot;ausentes&quot; nos
            manuscritos mais antigos não significa que não existiam — apenas que os
            manuscritos que os continham não sobreviveram. Consulte a{" "}
            <Link href="/timeline" className="text-primary hover:underline">
              Timeline
            </Link>{" "}
            para visualizar a evolução da cobertura por século.
          </>
        ),
      },
      {
        question: "Existem livros do NT que só aparecem no século IV?",
        answer: (
          <>
            Não há livros do NT que só apareçam a partir do século IV. Todos os 27
            livros possuem atestação manuscrita anterior. No entanto, livros
            menores como 2 João, 3 João e Judas têm menos testemunhos antigos
            simplesmente por serem curtos (1 capítulo) — há menos oportunidades de
            preservação. Os evangelhos e as epístolas paulinas têm atestação
            papirológica sólida desde o século II.
          </>
        ),
      },
      {
        question: "O que é a Perícope da Mulher Adúltera?",
        answer: (
          <>
            A passagem de João 7:53–8:11 (Pericope Adulterae) não aparece nos
            manuscritos mais antigos, como P66, P75, Sinaiticus e Vaticanus. É
            considerada pela maioria dos estudiosos como uma adição posterior ao
            evangelho de João, embora possivelmente preserve uma tradição
            autêntica. Este sistema registra a cobertura exata dos versículos,
            permitindo verificar em quais manuscritos a passagem aparece.
          </>
        ),
      },
      {
        question: "Qual é o manuscrito mais antigo do Novo Testamento?",
        answer: (
          <>
            O P52 (Papiro Rylands 457) é geralmente considerado o manuscrito mais
            antigo do NT, datado entre 125–175 d.C. Contém fragmentos de João
            18:31–33 e 18:37–38. Outros papiros antigos incluem P90 (João,
            séc. II), P104 (Mateus, séc. II) e o conjunto dos Papiros Chester
            Beatty (P45, P46, P47) do século III. Explore os detalhes na página{" "}
            <Link href="/manuscripts" className="text-primary hover:underline">
              Explorer
            </Link>
            .
          </>
        ),
      },
    ],
  },
  {
    title: "Perguntas comuns de um apologeta cristão",
    items: [
      {
        question:
          "Quantos manuscritos temos comparado a outras obras antigas?",
        answer: (
          <>
            O NT possui mais de 5.800 manuscritos gregos, além de ~10.000
            manuscritos em latim e milhares em outras línguas (siríaco, copta,
            etc.). Para comparação: a Ilíada de Homero tem ~1.800 manuscritos; as
            obras de Platão, ~250; os Anais de Tácito, ~20 cópias. Nenhuma obra
            da Antiguidade se aproxima do NT em volume de evidência manuscrita.
          </>
        ),
      },
      {
        question: "O NT é o texto antigo mais bem atestado?",
        answer: (
          <>
            Sim, tanto em número de manuscritos quanto em proximidade temporal com
            o original. O intervalo entre a composição dos livros do NT (c. 50–100
            d.C.) e os primeiros manuscritos (c. 125–200 d.C.) é de apenas
            25–150 anos. Para a maioria das obras clássicas, esse intervalo é de
            800–1400 anos.
          </>
        ),
      },
      {
        question: "Qual o século de estabilização textual?",
        answer: (
          <>
            O conceito de &quot;estabilização&quot; indica o século em que a cobertura
            cumulativa atinge 90% dos versículos. Para o NT como um todo, isso
            geralmente ocorre entre os séculos IV e V. Consulte as{" "}
            <Link href="/metrics" className="text-primary hover:underline">
              Métricas Acadêmicas
            </Link>{" "}
            para ver o século de estabilização por livro individual.
          </>
        ),
      },
      {
        question: "Qual o percentual de cobertura até o século III?",
        answer: (
          <>
            A cobertura varia por livro. Os evangelhos de João e Mateus têm
            cobertura significativa graças a papiros como P66, P75, P45. Epístolas
            paulinas também são bem atestadas via P46. Livros menores como 2 Pedro
            e Judas têm menos atestação nesse período. Use o{" "}
            <Link href="/dashboard" className="text-primary hover:underline">
              Dashboard
            </Link>{" "}
            com o slider de século para explorar os dados em tempo real.
          </>
        ),
      },
    ],
  },
  {
    title: "Perguntas técnicas acadêmicas",
    items: [
      {
        question: "O que é Fragmentation Index?",
        answer: (
          <>
            O Fragmentation Index mede o quão fragmentária é a atestação de um
            livro. Calculado como{" "}
            <code className="text-xs bg-secondary px-1 py-0.5 rounded">
              1 - (1 / avg_manuscripts_per_verse)
            </code>
            . Valores próximos de 0 indicam poucos manuscritos por versículo
            (alta fragmentação); valores próximos de 1 indicam muitos manuscritos
            atestando cada versículo (baixa fragmentação). Disponível na página{" "}
            <Link href="/metrics" className="text-primary hover:underline">
              Métricas
            </Link>
            .
          </>
        ),
      },
      {
        question: "O que é Century Growth Rate?",
        answer: (
          <>
            Taxa de crescimento da cobertura entre séculos consecutivos. Calculada
            como{" "}
            <code className="text-xs bg-secondary px-1 py-0.5 rounded">
              (coverage_N - coverage_N-1) / coverage_N-1 * 100
            </code>
            . Um growth rate alto no século III→IV reflete a contribuição dos
            grandes códices unciais. Taxas decrescentes indicam que a cobertura
            está se estabilizando.
          </>
        ),
      },
      {
        question: "O que é Coverage Density?",
        answer: (
          <>
            Relação entre versículos cobertos e número de manuscritos:{" "}
            <code className="text-xs bg-secondary px-1 py-0.5 rounded">
              covered_verses / manuscript_count
            </code>
            . Valores altos indicam que cada manuscrito contribui com muitos
            versículos únicos. Valores baixos indicam redundância (muitos
            manuscritos cobrindo os mesmos versículos).
          </>
        ),
      },
      {
        question: "Como funciona a cobertura cumulativa?",
        answer: (
          <>
            A cobertura cumulativa do século N inclui todos os manuscritos dos
            séculos 1 até N. Um versículo é &quot;coberto&quot; se aparece em pelo menos
            um manuscrito até o século considerado. Cada versículo é contado apenas
            uma vez, independente de quantos manuscritos o contenham. Isso
            significa que a cobertura é monotonicamente crescente — nunca diminui
            de um século para o próximo. Visualize na{" "}
            <Link href="/timeline" className="text-primary hover:underline">
              Timeline
            </Link>
            .
          </>
        ),
      },
    ],
  },
];

export default function FaqPage() {
  return (
    <div className="min-h-screen">
      <Header
        title="FAQ Acadêmico"
        subtitle="Perguntas frequentes sobre manuscritologia do NT"
      />

      <div className="p-6 space-y-8 max-w-3xl">
        {FAQ_DATA.map((category) => (
          <section key={category.title}>
            <h2 className="text-base font-semibold mb-3">{category.title}</h2>
            <div className="space-y-2">
              {category.items.map((item) => (
                <AccordionItem key={item.question} item={item} />
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}

function AccordionItem({ item }: { item: FaqItem }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="rounded-xl border border-border bg-card overflow-hidden">
      <button
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between px-5 py-4 text-left text-sm font-medium hover:bg-muted/30 transition-colors"
      >
        <span>{item.question}</span>
        <ChevronDown
          className={cn(
            "h-4 w-4 shrink-0 text-muted-foreground transition-transform",
            open && "rotate-180"
          )}
        />
      </button>
      {open && (
        <div className="border-t border-border px-5 py-4 text-sm text-muted-foreground leading-relaxed">
          {item.answer}
        </div>
      )}
    </div>
  );
}
