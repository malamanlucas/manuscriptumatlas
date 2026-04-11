---
name: testing
description: Testing pyramid, patterns, and standards for Manuscriptum Atlas — Vitest + Testing Library (frontend), Ktor test client + Kotest (backend), Playwright E2E.
---

# Testing — Padrões e Pirâmide

## Pirâmide de testes

```
         /‾‾‾‾‾‾‾‾‾\
        /   E2E (5%)  \        Playwright — fluxos críticos
       /‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\
      / Integração (25%) \     Backend: Ktor test client + DB real
     /‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\
    /    Unitários (70%)   \   Frontend: Vitest + Testing Library
   /‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\
```

## Frontend — Vitest + Testing Library

```bash
cd frontend && npm run test        # watch mode
cd frontend && npm run test:ci     # CI (sem watch)
```

### Padrões

```tsx
// components/coverage/BookCard.test.tsx
import { render, screen } from '@testing-library/react'
import { BookCard } from './BookCard'

describe('BookCard', () => {
  it('exibe nome do livro', () => {
    render(<BookCard book={{ name: 'Mateus', coverage: 0.95 }} />)
    expect(screen.getByText('Mateus')).toBeInTheDocument()
  })

  it('mostra skeleton enquanto carrega', () => {
    render(<BookCard book={null} isLoading />)
    expect(screen.getByRole('status')).toBeInTheDocument()
  })
})
```

- Testar **comportamento**, não implementação (não testar nomes de classes)
- Queries por acessibilidade: `getByRole`, `getByLabelText`, `getByText`
- Evitar `getByTestId` — usar apenas como último recurso
- Mocks: `vi.mock('../hooks/useCoverage')` para isolar da API
- Cada teste: arrange → act → assert (3 linhas de clareza)

### O que testar no frontend

- Renderização de estados: loading, error, empty, populated
- Interações do usuário: click, submit, navegação
- Formatação de dados (verseReference, datas, percentuais)
- Hooks customizados com `renderHook`

## Backend — Ktor Test Client + Kotest

```bash
cd backend && ./gradlew test
cd backend && ./gradlew test --tests "*.CoverageRoutesTest"
```

### Padrões

```kotlin
// routes/CoverageRoutesTest.kt
class CoverageRoutesTest : FunSpec({
    val app = TestApplication {
        application { configureRouting() }
    }

    test("GET /coverage retorna lista paginada") {
        app.client.get("/coverage?page=1&limit=10").apply {
            status shouldBe HttpStatusCode.OK
            val body = body<List<CoverageDto>>()
            body.size shouldBeLessThanOrEqualTo 10
        }
    }

    test("GET /coverage/{book} retorna 404 para livro inexistente") {
        app.client.get("/coverage/LIVRO_INEXISTENTE").apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }
})
```

- **Nunca mockar o banco** — usar DB de teste real (H2 ou PostgreSQL de teste)
- Testar rotas via `TestApplicationEngine` (não unitário de serviço)
- Verificar: status HTTP, estrutura do body, paginação, locale
- Seeds de teste: criar fixtures minimais, não depender do seed de produção

### O que testar no backend

- Rotas (status codes, body shape, auth required)
- Queries de repositório (paginação, filtros, ordenação)
- Lógica de cobertura cumulativa
- Seed idempotência (rodar 2x não duplica)

## E2E — Playwright

```bash
cd frontend && npm run test:e2e           # headless
cd frontend && npm run test:e2e -- --ui   # com UI
```

### Screenshots obrigatórias para validação visual

```typescript
// e2e/dashboard.spec.ts
test('dashboard carrega em desktop e mobile', async ({ page }) => {
  await page.goto('/pt/dashboard')
  await page.waitForLoadState('networkidle')

  // Desktop
  await page.setViewportSize({ width: 1280, height: 800 })
  await expect(page).toHaveScreenshot('dashboard-desktop.png')

  // Mobile
  await page.setViewportSize({ width: 375, height: 812 })
  await expect(page).toHaveScreenshot('dashboard-mobile.png')
})
```

### Fluxos críticos a cobrir com E2E

- Dashboard: carregamento de gráficos e métricas
- Manuscripts: listagem, busca, detalhe de manuscrito
- Councils: mapa interativo, detalhe de concílio
- Ingestion: status das fases (admin autenticado)
- Bible: leitura, interlinear, busca Strong's

## Checklist antes de PR

- [ ] Testes unitários passando (`npm run test:ci` / `./gradlew test`)
- [ ] Nenhum teste novo com `vi.mock` de banco — usar DB real no backend
- [ ] Screenshot E2E atualizada se houver mudança visual
- [ ] Novos componentes com ≥1 teste de render + 1 de interação
- [ ] Novos endpoints com ≥1 teste de sucesso + 1 de erro (404/400)
