---
name: frontend-patterns
description: General frontend patterns for Next.js/React/TanStack. Use when working on componente, React, hook, página, frontend, TypeScript, or TanStack.
---

# Frontend Patterns

Complementa `frontend-conventions.mdc`. Para responsividade, ver skill `frontend-responsiveness`.

## Data fetching

- TanStack Query: `useQuery`, `useMutation`
- Funções de fetch em `lib/api.ts`
- API via rewrite `/api/*` → backend

## Hooks

Um hook por domínio em `hooks/`: `useCouncils`, `useChurchFathers`, `useCoverage`, etc.

## Componentes

- Server Components por padrão
- `"use client"` apenas quando necessário (estado, efeitos, event handlers)

## Tipos

Centralizados em `types/index.ts`. Evitar `any`.

## Estilização

Tailwind utility-first. Dark mode via prefixo `dark:`.

## Checklist por feature

1. i18n — 3 arquivos de mensagens (en, pt, es)
2. Responsividade — mobile, tablet, desktop (ver `frontend-responsiveness`)
3. Tema — light e dark mode
4. Tipos — TypeScript sem `any`
5. Loading/Error — estados tratados
