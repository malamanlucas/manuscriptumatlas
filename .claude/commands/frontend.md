Você está trabalhando no frontend Next.js/React/TypeScript do Manuscriptum Atlas. Siga estas regras:

## Padrões
- Server Components por padrão; `"use client"` apenas com estado/efeitos/event handlers
- Data fetching: TanStack Query (`useQuery`, `useMutation`), funções em `lib/api.ts`
- Tipos centralizados em `types/index.ts`, evitar `any`
- Tailwind utility-first, dark mode via `dark:`

## Responsividade obrigatória
| Tipo de página | max-width | Tailwind |
|----------------|-----------|----------|
| Dashboard/admin | Wide | `max-w-7xl` + `mx-auto w-full` |
| Conteúdo | Medium | `max-w-3xl` ou `max-w-5xl` |
| Forms/modais | Narrow | `max-w-sm` a `max-w-md` |

- Grid adaptativo: `grid-cols-1 md:grid-cols-2 lg:grid-cols-3`
- Tabelas: `overflow-x-auto`
- Padding escalável: `p-4 md:p-6`
- Testar em 320px

## Design System (`.interface-design/system.md`)

Antes de construir qualquer componente visual, leia `.interface-design/system.md`. Ele contém os tokens, padrões e decisões de design do projeto. Aplique-os consistentemente.

### Princípios visuais
- **Personalidade:** Scholarly Precision — ferramenta acadêmica com densidade de dados
- **Depth:** Borders + surface elevation (`border-border bg-card`), sem drop shadows pesados
- **Paleta:** Slate (cool), semantic colors (emerald=success, amber=warning, red=error, blue=info, purple=metrics)
- **Superfícies:** Cards com `rounded-xl border border-border bg-card p-4 md:p-5`
- **Tipografia:** system-ui, text-sm base, text-2xl font-bold para stat values, tabular-nums para dados

### Padrões de componentes reutilizáveis
- **Stat Card:** rounded-xl + icon com bg colorido (rounded-lg p-2) + valor text-2xl + label text-xs
- **Section Card:** rounded-xl + heading text-base font-semibold com icon + conteúdo
- **Tabela:** rounded-xl border + thead text-xs muted + tbody divide-y + hover:bg-muted/30
- **Progress Bar:** h-2 rounded-full bg-muted + fill com cor semântica
- **Loading:** skeleton animate-pulse ou Loader2 animate-spin
- **Error:** rounded-xl border-red bg-red-50 dark:bg-red-950

### Anti-padrões (evitar)
- Drop shadows pesados — usar borders
- Bordas harsh (alta opacidade) — manter sutis
- Cores decorativas sem significado — cor = informação
- Spacing inconsistente — seguir a escala (4, 8, 12, 16, 24, 32)
- Cards com cantos sharp misturados com rounded — manter rounded-xl
- Saltos dramáticos de superfície entre níveis

## Checklist por feature
1. i18n — 3 arquivos de mensagens (en, pt, es)
2. Responsividade — mobile, tablet, desktop
3. Tema — light e dark mode
4. Tipos — TypeScript sem `any`
5. Loading/Error — estados tratados
6. Consistência — tokens e padrões do `system.md` aplicados

Analise o pedido do usuário: $ARGUMENTS
