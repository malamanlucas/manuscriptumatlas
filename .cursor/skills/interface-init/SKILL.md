---
name: interface-init
description: UI craft principles for Manuscriptum Atlas — intentional design, visual audit checklist, and component quality standards.
---

# Interface Init — UI Craft

Princípios para construir UI com craft e consistência. Aplicar sempre que criar ou editar componentes, páginas ou layouts.

## Mentalidade: intenção primeiro

Cada decisão visual deve ter uma razão. Antes de adicionar padding, escolher uma cor ou definir um tamanho de texto, perguntar: **por que esse valor?** UI gerada por default (valores arbitrários, sem ritmo visual) não passa no bar de qualidade do Atlas.

## Hierarquia visual

- **Tamanho** define importância: título > label > body > caption
- **Peso** reforça hierarquia: `font-semibold` para labels de seção, `font-medium` para itens, `font-normal` para corpo
- **Cor** comunica estado: default/muted/destructive/accent — não usar cor para decoração
- **Espaço** separa grupos, une relacionados: padding interno coeso, gap entre grupos maior

## Densidade e layout

- Dashboards: `max-w-7xl mx-auto w-full` com padding `px-4 sm:px-6 lg:px-8`
- Cards: `rounded-lg border bg-card p-4 md:p-6` — consistente em todo o app
- Grid adaptativo: `grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4`
- Tabelas: `w-full overflow-x-auto` — nunca quebrar em mobile sem scroll

## Checklist de audit visual por componente

Antes de marcar implementação como concluída:

- [ ] **Hierarquia legível** — olho segue um caminho claro (heading → sub → body)
- [ ] **Dark mode** — verificar com `dark:` classes; nada "desaparece" no dark
- [ ] **Mobile (320px)** — sem overflow horizontal, texto legível, touch targets ≥ 44px
- [ ] **Empty state** — o que aparece quando não há dados? (skeleton ou mensagem)
- [ ] **Loading state** — spinner ou skeleton adequado ao contexto
- [ ] **Error state** — mensagem amigável, não stack trace
- [ ] **Sem magic numbers** — usar escala Tailwind (4, 6, 8, 10, 12, 16...) não `p-[13px]`
- [ ] **Cores semânticas** — `text-muted-foreground` para secundário, não `text-gray-400`

## Padrões de componente

```tsx
// Card padrão
<div className="rounded-lg border bg-card p-4 md:p-6 space-y-4">
  <h2 className="text-lg font-semibold">Título</h2>
  <p className="text-sm text-muted-foreground">Descrição</p>
</div>

// Badge de estado
<span className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium
  bg-primary/10 text-primary">
  Ativo
</span>

// Empty state
<div className="flex flex-col items-center justify-center py-12 text-center">
  <p className="text-sm text-muted-foreground">Nenhum resultado encontrado</p>
</div>
```

## Anti-padrões a evitar

- `text-gray-*` hardcoded — usar variáveis semânticas do design system
- `p-[valor-arbitrário]` — usar escala Tailwind
- Componente sem estado de loading/empty
- Layout que quebra em 375px (iPhone SE)
- Contraste insuficiente em dark mode (verificar com DevTools)
- Misturar tamanhos de texto sem critério (h1 + h2 + h3 + p todos em sequência)
