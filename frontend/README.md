# Manuscriptum Atlas — Frontend

Frontend do Manuscriptum Atlas, construído com Next.js 16 (App Router), React 19 e TypeScript.

## Stack

- **Next.js 16** com App Router e Server Components
- **React 19** com componentes funcionais
- **TypeScript 5.9**
- **Tailwind CSS 4** para estilização
- **TanStack React Query 5** para data fetching e cache
- **Recharts 3** para visualizações e gráficos
- **next-intl** para internacionalização (pt, en, es)
- **next-themes** para tema (light/dark/system)
- **Lucide React** para ícones

## Execução

```bash
npm install
npm run dev
```

O frontend roda em http://localhost:3000 e faz proxy de `/api/*` para o backend em http://localhost:8080.

Em Docker, o backend é acessível via `http://app:8080` (variável `BACKEND_URL`).

## Estrutura

```
frontend/
├── app/
│   ├── [locale]/              20 páginas com i18n
│   │   ├── dashboard/         Dashboard com estatísticas globais
│   │   ├── manuscripts/       Explorer de manuscritos + detalhe [gaId]
│   │   ├── book/[name]/       Detalhe de livro
│   │   ├── timeline/          Timeline evolutiva
│   │   ├── compare/           Comparação de cobertura
│   │   ├── metrics/           Métricas acadêmicas + detalhe [book]
│   │   ├── verse-lookup/      Busca de versículos
│   │   ├── manuscript-count/  Contagem por tipo
│   │   ├── fathers/           Pais da Igreja (lista, [id], testimony)
│   │   ├── history/           Conteúdo educacional
│   │   ├── sources/           Fontes e referências
│   │   ├── ingestion-status/  Status da ingestão
│   │   └── faq/               Perguntas frequentes
│   ├── globals.css
│   ├── layout.tsx
│   └── page.tsx
├── components/
│   ├── charts/                TimelineChart, Heatmap, ComparisonChart, FathersTimelineChart
│   ├── coverage/              BookCard, CenturySlider, GospelPanel, VerseGrid
│   ├── layout/                Header, Sidebar, SidebarContext, LanguageSelector, ThemeToggle
│   ├── stats/                 StatsOverview
│   └── providers.tsx          React Query + Theme providers
├── hooks/                     9 hooks (useCoverage, useManuscripts, useChurchFathers, etc.)
├── lib/                       api.ts (25 funções fetch), utils.ts
├── types/                     index.ts (32 interfaces/tipos)
└── messages/                  en.json, pt.json, es.json
```

## Convenções

- Hooks customizados um por domínio em `hooks/`
- Funções de API centralizadas em `lib/api.ts`
- Tipos centralizados em `types/index.ts`
- Internacionalização via `useTranslations()` do next-intl
- Ao adicionar features, atualizar os 3 arquivos de mensagens
