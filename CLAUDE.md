# Manuscriptum Atlas

Sistema de cobertura textual do Novo Testamento grego — manuscritos, testemunhos patrísticos, concílios e heresias.

## Stack

- **Backend:** Kotlin 2.1 / Ktor 3.1 / Exposed ORM / PostgreSQL 16 / Flyway
- **Frontend:** Next.js 16 / React 19 / TypeScript / Tailwind CSS 4 / TanStack Query / next-intl
- **LLM:** LLM Queue (PostgreSQL `llm_prompt_queue`) processada por Claude Code via `/run-llm` — tiered (Haiku/Sonnet/Opus). Fallback síncrono `LlmOrchestrator` (Anthropic → OpenAI → DeepSeek → OpenRouter) para endpoints request-scoped (ex: apologetics).
- **Messaging:** Apache Kafka (KRaft mode) — notificacao de resultados LLM processados
- **Infra:** Docker Compose (postgres + kafka + init + app + frontend + prometheus + grafana + loki + promtail)
- **Observability:** Prometheus (metrics) + Grafana (dashboards, porta 3001) + Loki (logs) + Micrometer + structured JSON logging
- **Arquitetura:** Routes → Service → Repository → Database (Exposed/PostgreSQL)

## Tier → Modelo (LLM Queue)

| Tier | Fases | Modelo |
|------|-------|--------|
| LOW | fases que **terminam** em `_enrichment` (geram enriquecimento novo, texto curto) | Haiku (`claude-haiku-4-5`) |
| MEDIUM | `bible_translate_*` (inclui `bible_translate_enrichment_greek/hebrew`), `council_*`, `heresy_*`, `bio_*` | Sonnet (`claude-sonnet-4-6`) |
| HIGH | `bible_align_*`, `dating_*`, `apologetics_*` | Opus (`claude-opus-4-7`) |

Regra rápida: `translate` no nome → Sonnet (prevalece sobre `enrichment`); `tier=HIGH` → Opus; `*_enrichment` (sem `translate`) → Haiku. Detalhes em `/run-llm`.

## Checklist obrigatório por feature

1. **i18n** — textos nos 3 locales (pt, en, es); backend com `?locale=`
2. **Responsividade** — mobile (320px), tablet, desktop
3. **Tema** — light e dark mode
4. **Tipos** — TypeScript sem `any`
5. **Loading/Error** — estados tratados

## Carregamento inteligente de skills

Ao receber um prompt de **implementação ou alteração de código**, detecte o contexto e carregue o command sob demanda via Skill. **Não carregar** para dúvidas, explicações ou perguntas conceituais.

| Contexto detectado | Skill | Carrega |
|-------------------|-------|---------|
| Kotlin, Ktor, endpoint, service, repository, route | `/backend` | Convenções Kotlin, armadilhas, checklist novo domínio |
| Tabela, migration, schema, coluna, PostgreSQL, Flyway | `/database` | Alerta Flyway, procedimentos SchemaUtils |
| Componente, React, hook, página, layout, responsividade | `/frontend` | Padrões Next.js, design system, responsividade |
| UI craft, design, interface, visual, audit | `/interface-init` | Princípios de craft, intenção-primeiro |
| Tradução, locale, i18n, mensagens, idioma | `/i18n` | Regras de sincronização 3 locales |
| Ingestão, ingestion, pipeline, scraper, fases | `/ingestion` | Fases, SourceConsensusEngine, endpoints admin |
| Docker, compose, infra, deploy, variável de ambiente | `/docker` | Serviços, volumes, env vars, comandos dev |
| Testes, test, E2E, Playwright, Vitest, pirâmide | `/testing` | Pirâmide, padrões frontend/backend |
| Drenar fila LLM (volume alto) | `/drain-queue` | Wrapper safe com batch=50 parallelism=5 |
| Stats da fila (read-only) | `/queue-status` | ETA, stale claims, rate-limit ativo |
| Processar 1 lote curto | `/run-llm` | Motor LLM — spawna Agents Haiku/Sonnet |
| Validar integridade pós-drenagem | `/integrity-check` | Phantom applied, orphan claims, callback_context |

**Regras de dispatch:**
- Tarefa cross-layer (ex: "novo domínio"): carregar skill principal, seguir ordem arquitetura → banco → backend → frontend → i18n
- Múltiplas skills necessárias: carregar a principal e referenciar as secundárias conforme avança
- Dúvida conceitual: responder direto, sem carregar skill

## Uso de subagents (paralelismo)

Para maximizar performance, usar subagents em paralelo quando possível:

- **Exploração do codebase**: spawnar Explore agents paralelos para investigar áreas independentes
- **Tarefas multi-camada**: um agent pesquisa backend enquanto outro explora frontend
- **Pesquisa + implementação**: agent pesquisa padrões existentes enquanto o principal planeja
- **Validação**: agent roda testes enquanto outro verifica tipos

**Quando NÃO usar subagent:**
- Tarefa trivial (1-2 arquivos, mudança localizada)
- Leitura de arquivo específico (usar Read direto)
- Busca simples (usar Grep/Glob direto)

## MCP — ferramentas externas

- **Playwright**: validação visual após alterações frontend — screenshot em desktop (1280px) e mobile (375px)
- **PostgreSQL**: queries diretas para debug de dados
- **Fetch**: testar APIs locais (localhost)

Playwright: usar automaticamente após mudanças visuais. Não usar para lógica sem impacto visual.

## LLM Queue Processing

- Batch sizes: **claim** = 50 (`/drain-queue` default), **display dashboard** = 10, **single-agent** = 1 (`/run-llm`).
- Parallelism **MAX = 5 sub-agents simultâneos**. Nunca spawn 30+.
- Antes de spawn > 5 agents: pare e pergunte.
- Temp files devem ser **PID-scoped**: `/tmp/llm_item_$$_*.json` — nunca caminho compartilhado tipo `/tmp/llm_item_*.json`.
- Caracteres especiais (ex: "pagaré", "João", "עִבְרִית") → usar Python `json.dumps` + `curl -d @/tmp/body_$$.json`, nunca inline bash strings.
- Esperar **format drift** de Haiku (array vs object, text vs JSON fence) — normalize (`jq empty`) antes de `POST /complete`.

## Rate Limits (Claude plan: America/Sao_Paulo)

- Usage limit reseta às **17:00 America/Sao_Paulo** (20:00 UTC).
- Antes de iniciar cron longo, chequear `/tmp/claude_rate_limit_until` — se timestamp futuro, abortar silencioso.
- Se `/run-llm` bloqueia por usage limit: **não retry em loop apertado** — escreva timestamp do próximo reset em `/tmp/claude_rate_limit_until`, cancele o cron, avise o usuário.
- Graceful shutdown: cancelar cron + `curl -X POST /admin/llm/queue/unstick?staleMinutes=0` para liberar items em `processing`.

## Autenticação Admin (backend local)

Rotas `/admin/*` exigem `Authorization: Bearer <token>`. Obter token:

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/dev-login?email=dev@manuscriptum.local" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

Usar nas chamadas:
```bash
curl -s -X POST http://localhost:8080/admin/<endpoint> -H "Authorization: Bearer $TOKEN"
```

- Usuário dev admin: `dev@manuscriptum.local` (Admin) — não requer senha, só funciona com `JWT_SECRET` dev
- Endpoints úteis: `POST /admin/bible/ingestion/run/{phase}`, `POST /admin/llm/queue/apply/{phase}`, `POST /admin/llm/queue/unstick`

## Idioma

- Código-fonte, variáveis e classes: **inglês**
- Comunicação com o usuário: **português**
