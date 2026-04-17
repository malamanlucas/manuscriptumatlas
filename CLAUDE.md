# Manuscriptum Atlas

Sistema de cobertura textual do Novo Testamento grego — manuscritos, testemunhos patrísticos, concílios e heresias.

## Stack

- **Backend:** Kotlin 2.1 / Ktor 3.1 / Exposed ORM / PostgreSQL 16 / Flyway
- **Frontend:** Next.js 16 / React 19 / TypeScript / Tailwind CSS 4 / TanStack Query / next-intl
- **LLM:** Anthropic Claude Opus 4.7 (primário) + OpenAI GPT-5.4 / DeepSeek / OpenRouter (fallback) via `LlmOrchestrator` + LLM Queue (Claude Code `/run-llm`)
- **Messaging:** Apache Kafka (KRaft mode) — notificacao de resultados LLM processados
- **Infra:** Docker Compose (postgres + kafka + init + app + frontend + prometheus + grafana + loki + promtail)
- **Observability:** Prometheus (metrics) + Grafana (dashboards, porta 3001) + Loki (logs) + Micrometer + structured JSON logging
- **Arquitetura:** Routes → Service → Repository → Database (Exposed/PostgreSQL)

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

## Idioma

- Código-fonte, variáveis e classes: **inglês**
- Comunicação com o usuário: **português**
