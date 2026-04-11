# Plano: Reorganização e Limpeza do Manuscriptum Atlas

## Contexto

O projeto acumulou arquivos temporários na raiz (screenshots, scripts, dumps, CSVs), tem um `.gitignore` com apenas 11 linhas, dois arquivos de migração V18 em conflito, e dois monólitos no frontend (`api.ts` com 796 linhas e `types/index.ts` com 820 linhas) que dificultam a navegação. O objetivo é organizar a estrutura sem quebrar funcionalidade.

---

## Fase 1 — Limpeza da Raiz (risco: baixo)

### 1.1 Deletar screenshots temporários
```
rm dashboard-sober-palette.png fathers-sober-palette.png fathers-chart-links-final.png fathers-timeline-links.png
```

### 1.2 Mover scripts shell para `scripts/`
| Atual | Destino |
|-------|---------|
| `up.sh` | `scripts/up.sh` |
| `up.dev.sh` | `scripts/up.dev.sh` |
| `up.prod.sh` | `scripts/up.prod.sh` |
| `up.db.prod.sh` | `scripts/up.db.prod.sh` |
| `backup.sh` | `scripts/backup.sh` |
| `restore.sh` | `scripts/restore.sh` |

- Atualizar referências internas nos scripts (path relativo `SCRIPT_DIR`)
- Atualizar `README.md` e `CLAUDE.md` com novos caminhos

### 1.3 Mover docs soltos para `docs/`
| Atual | Destino |
|-------|---------|
| `PLAN-jwt-interno.md` | `docs/plans/PLAN-jwt-interno.md` |
| `cursor.md` | `.cursor/cursor.md` (config IDE, não precisa estar na raiz) |

### 1.4 Deletar arquivos obsoletos da raiz
- `database.csv` — schema antigo, supersedido por `Tables.kt`
- `anthropic-key.txt`, `open-api-key.txt` — redundantes (já estão no docker-compose e no `.env`)

---

## Fase 2 — `.gitignore` Abrangente (risco: baixo)

Substituir o `.gitignore` atual (11 linhas) por uma versão completa:

```gitignore
# Build
build/
bin/
out/
*.class
.gradle/
.kotlin/

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db

# Secrets
.env
.env.*
!.env.example
*-key.txt
password.txt

# Database backups
backups/*.dump

# Screenshots temporários
*.png
!frontend/public/**/*.png

# Monitoring
newrelic.yml

# IDE-specific docs
cursor.md
database.csv

# Node
frontend/node_modules/

# Playwright MCP artifacts
.playwright-mcp/

# Logs
*.log
```

Remover do index git (sem deletar do disco):
```bash
git rm --cached .DS_Store .cursor/.DS_Store .cursor/rules/.DS_Store
git rm --cached cursor.md database.csv newrelic.yml
git rm --cached backups/*.dump
git rm --cached PLAN-jwt-interno.md
```

---

## Fase 3 — Fix Migração V18 Duplicada (risco: baixo)

**Problema:** Dois arquivos com versão V18:
- `V18__create_llm_usage_logs.sql`
- `V18__create_bible_db_schema.sql`

**Ação:**
1. Renomear `V18__create_bible_db_schema.sql` → `V19__create_bible_db_schema.sql`
2. Atualizar CLAUDE.md: "Próxima migração: **V20**"
3. Atualizar `memory/MEMORY.md` correspondentemente

---

## Fase 4 — Split `frontend/types/index.ts` (risco: médio)

Dividir 820 linhas em módulos por domínio, com barrel re-export para backward compatibility.

**Nova estrutura:**
```
frontend/types/
  index.ts          ← barrel: export * from cada módulo
  common.ts         ← TimelineEntry, ChapterCoverage, MissingVerse, PaginatedResponse
  manuscripts.ts    ← ManuscriptSummary, ManuscriptDetailResponse, BookRanges
  coverage.ts       ← BookCoverage, CoverageSummary, CenturyCoverageResponse
  metrics.ts        ← BookMetricsResponse, NtMetricsResponse, ManuscriptsCountResponse
  fathers.ts        ← ChurchFatherSummary, ChurchFatherDetail, TextualStatementDTO
  councils.ts       ← CouncilSummaryDTO, CouncilDetailDTO, CouncilCanonDTO
  heresies.ts       ← HeresySummaryDTO, HeresyDetailDTO
  sources.ts        ← SourceDTO, SourceClaimDTO
  ingestion.ts      ← IngestionStatusResponse, PhaseStatusDTO, CacheStatsDTO
  analytics.ts      ← AnalyticsOverview, VisitorSession*, SessionFilters
  auth.ts           ← UserDTO, LoginResponse
  llm.ts            ← LlmUsageLogDTO, LlmProviderSummaryDTO, RateLimiterStatusDTO
  bible.ts          ← BibleVersionDTO, BibleBookDTO, InterlinearWordDTO
```

**Barrel `index.ts`:**
```typescript
export * from './common';
export * from './manuscripts';
export * from './coverage';
// ... cada módulo
```

Todos os imports existentes `from "@/types"` continuam funcionando sem alteração.

**Verificação:** `npx tsc --noEmit` + `npm run build`

---

## Fase 5 — Split `frontend/lib/api.ts` (risco: médio)

Dividir 796 linhas / 98 funções em módulos por domínio.

**Nova estrutura:**
```
frontend/lib/api/
  index.ts          ← barrel re-export
  client.ts         ← fetchJson, fetchJsonAuth, buildParams, token mgmt, AuthError
  auth.ts           ← loginWithGoogle, getAuthMe, getUsers, createUser, etc.
  coverage.ts       ← getCoverage, getCoverageByCentury, getChapterCoverage
  timeline.ts       ← getTimeline, getTimelineFull
  manuscripts.ts    ← getManuscripts, getManuscriptDetail, getManuscriptsForVerse
  metrics.ts        ← getNtMetrics, getBookMetrics, getManuscriptsCount, getStatsOverview
  fathers.ts        ← getChurchFathers, getChurchFatherDetail, searchChurchFathers
  councils.ts       ← getCouncils, searchCouncils, getCouncilDetail
  heresies.ts       ← getHeresies, getHeresyDetail
  sources.ts        ← getSources
  ingestion.ts      ← ingestion status, phases, run, reset, enrichment
  analytics.ts      ← visitor analytics (overview, sessions, heatmap, etc.)
  llm.ts            ← fetchLlmUsageDashboard, fetchLlmUsageLogs, fetchLlmRateLimits
  bible.ts          ← all Bible API functions
```

Cada módulo importa `fetchJson`/`fetchJsonAuth` de `./client`. O barrel `index.ts` re-exporta tudo.

**Verificação:** `npm run build` + testar 3-4 páginas no browser

---

## Fase 6 — Consolidação de Componentes (risco: baixo)

| Arquivo | Ação |
|---------|------|
| `components/tracking/VisitorTracker.tsx` | Mover para `components/observatory/` |
| `components/tracking/` | Deletar pasta vazia |

Manter `components/stats/StatsOverview.tsx` onde está — é usado em dashboard e patristic-dashboard, faz sentido como módulo separado.

---

## Fase 7 — Atualizar Documentação

- `CLAUDE.md`: atualizar próxima migração (V20), caminhos de scripts, documentar split de api/types
- `README.md`: atualizar caminhos dos scripts

---

## O que NÃO fazer

- **NÃO refatorar sub-pacotes do backend Kotlin** — 24 services/repositories em pacote flat é aceitável para este tamanho de projeto. O custo de atualizar imports em 100+ arquivos Kotlin não compensa.
- **NÃO consolidar hooks de ingestion** — os 3 arquivos (useIngestion, useIngestionPhases, useCouncilIngestion) têm responsabilidades distintas e não se sobrepõem.
- **NÃO mover api keys do docker-compose.yml agora** — isso é uma mudança de infra que merece seu próprio plano.

---

## Ordem de Commits

| # | Escopo | Risco |
|---|--------|-------|
| 1 | Fase 1: limpeza da raiz (delete PNGs, mover scripts/docs) | Baixo |
| 2 | Fase 2: `.gitignore` + `git rm --cached` | Baixo |
| 3 | Fase 3: rename V18 → V19 | Baixo |
| 4 | Fase 4: split `types/index.ts` | Médio |
| 5 | Fase 5: split `lib/api.ts` | Médio |
| 6 | Fase 6: mover VisitorTracker | Baixo |
| 7 | Fase 7: atualizar docs | Baixo |

**Resultado final da raiz (limpa):**
```
manuscriptumatlas/
  .claude/           .cursor/         .git/
  backups/           deploy/          docs/
  frontend/          gradle/          nginx/
  scripts/           src/
  .dockerignore      .gitignore       build.gradle.kts
  CLAUDE.md          docker-compose.yml  docker-compose.prod.yml
  Dockerfile         gradle.properties   gradlew
  gradlew.bat        README.md        settings.gradle.kts
```

De ~48 itens na raiz → ~18 itens. Redução de 62%.
