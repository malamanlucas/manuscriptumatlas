---
name: backend-kotlin
description: Quick-fixes and pitfalls for Kotlin/Ktor/Exposed backend. Use when working on backend, Kotlin, Ktor, endpoints, services, repositories, or routes.
---

# Backend Kotlin — Quick Fixes

Complementa `kotlin-conventions.mdc`. Para convenções completas, ver essa rule.

## Armadilhas

**Rotas literais ANTES de dinâmicas** — no Ktor, rotas com path fixo devem vir antes das com parâmetros. Caso contrário: 404 silencioso.

```kotlin
get("/fathers/statements") { ... }   // ANTES
get("/fathers/{id}") { ... }         // DEPOIS
```

**`transaction {}` no Repository, nunca no Service** — Service delega ao Repository; Repository usa `transaction { }` ou `newSuspendedTransaction { }`.

**Serialização** — sempre `@Serializable` (kotlinx.serialization), nunca Jackson.

## Quick-fix: Novo endpoint

1. Route em `routes/` — recebe Service via construtor
2. Service em `service/` — recebe Repository via construtor
3. Repository em `repository/` — usa `transaction { }` para queries
4. Registrar no `Application.kt`: instanciar, conectar rotas

## Quick-fix: Novo domínio completo

Ver checklist de 10 passos em `kotlin-conventions.mdc` seção "Registrar novos domínios".

## Parsing de resposta LLM (padrão JSON-first)

Ao processar respostas LLM que devem ser JSON `{key: value}`:

1. **Tentar JSON parsing primeiro** (`tryParseJsonGlosses`) — key-based matching robusto
2. **Se JSON parcial**, retornar apenas os matched — nunca preencher gaps com line-by-line
3. **Se conteúdo parece JSON mas nada matchou**, retornar vazio (não cair em fallback posicional)
4. **Line-by-line** somente para respostas genuinamente plain-text (sem `{` ou `}`)

Referência: `BibleIngestionService.processGlossResponse()` — corrigido em 2026-04 para evitar
armazenamento de fragmentos JSON como dados válidos.
