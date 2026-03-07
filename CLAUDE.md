# Manuscriptum Atlas

Sistema de cobertura textual do Novo Testamento grego — manuscritos históricos, testemunhos patrísticos, concílios e heresias. Métricas de cobertura acumulativa por século/livro.

## Stack

- **Backend:** Kotlin 2.1 / Ktor 3.1 / Exposed ORM / PostgreSQL 16 / Flyway
- **Frontend:** Next.js 16 / React 19 / TypeScript / Tailwind CSS 4 / Recharts / Leaflet / TanStack Query / next-intl
- **Infra:** Docker Compose (postgres + init + app + frontend)

## Arquitetura

```
Routes → Service → Repository → Database (Exposed/PostgreSQL)
```

Pacotes `com.ntcoverage.*`: `config`, `model` (Tables + DTOs), `repository`, `service`, `routes`, `scraper`, `seed`, `util`.

## Convenções Kotlin

- Classes: `PascalCase` | Funções: `camelCase` | Tabelas Exposed: `PascalCase` plural | Enums: `UPPER_SNAKE_CASE`
- Serialização: `kotlinx.serialization` (`@Serializable`), nunca Jackson
- Transações: sempre via `transaction {}` do Exposed
- Async: Kotlin Coroutines (`suspend fun`, `async`, `withContext`)
- DI por construtor, sem frameworks
- Logging: `LoggerFactory.getLogger`
- Rotas literais (`/fathers/statements`) ANTES de dinâmicas (`/fathers/{id}`)
- Repository retorna DTOs, nunca ResultRows. SQL customizado via `object : Op<Boolean>()`
- Service recebe repositórios por construtor, não acessa `transaction {}` diretamente
- Retry com backoff para chamadas externas (NTVMR, OpenAI)

### Registrar novo domínio (checklist)

1. Table em `model/Tables.kt` (+ tabela de traduções se i18n)
2. DTOs em `model/DTOs.kt`
3. Repository em `repository/`
4. Service em `service/`
5. Routes em `routes/` (endpoints com `?locale=` se traduzível)
6. Registrar no `Application.kt`
7. Registrar Table no `FlywayConfig.kt` (SchemaUtils + índices extras)
8. Migração SQL em `src/main/resources/db/migration/`
9. Seed em `seed/` (dados + traduções pt/es) se aplicável
10. Frontend: chaves nos 3 arquivos de mensagens + layout responsivo

## Convenções Frontend

- Componentes funcionais TypeScript, hooks em `frontend/hooks/`, tipos em `frontend/types/index.ts`
- Preferir Server Components; `"use client"` apenas quando necessário
- Tipagem explícita — evitar `any`
- Data fetching: TanStack Query (`useQuery`, `useMutation`), funções em `lib/api.ts`
- Estilização: Tailwind utility-first, sem CSS modules/styled-components
- Responsividade obrigatória: mobile (≥320px), tablet, desktop
  - Dashboards/admin: `max-w-7xl` com `mx-auto w-full`
  - Grid adaptativo: `grid-cols-1 md:grid-cols-2 lg:grid-cols-3`
  - Tabelas largas: `overflow-x-auto`
  - Textos escaláveis: `text-sm md:text-base`, `p-4 md:p-6`
- i18n: `messages/{locale}.json` + `useTranslations()`. Nenhum texto hardcoded
- Ao adicionar features, atualizar os 3 arquivos de mensagens (pt, en, es) simultaneamente

## Convenções de Banco (Flyway)

**IMPORTANTE:** Flyway NÃO executa migrations em runtime. O fallback em `FlywayConfig.kt` cria o schema. Migrations `.sql` servem como documentação. Se algo só existir no `.sql`, NÃO será criado.

- Nomenclatura: `V{N}__{descricao_snake_case}.sql` (dois underscores)
- Sempre incremental — nunca alterar migrations já aplicadas
- PostgreSQL 16 — pode usar JSONB, generated columns, pg_trgm, CHECK constraints
- Próxima migração: **V18**

### Checklist — tabelas normais

1. Migração SQL `V{N}__...sql`
2. Objeto `Table` em `Tables.kt`
3. **Registrar no `SchemaUtils.createMissingTablesAndColumns()` em `FlywayConfig.kt`** ← erro mais comum: esquecer este passo causa `PSQLException: relation "xxx" does not exist` em runtime
4. Índices extras em `applyExtraIndexesAndConstraints()`

### Checklist — tabelas particionadas

SchemaUtils NÃO suporta particionamento. Criar via SQL raw no fallback:

1. Migração SQL `V{N}__...sql`
2. Objeto `Table` em `Tables.kt` (aponta para tabela pai)
3. **NÃO registrar** no SchemaUtils
4. Criar via `exec(SQL)` em `FlywayConfig.kt` (verificar existência com `pg_class`)
5. Criar partições iniciais (mês atual + 2 meses)
6. Criar função de auto-particionamento e índices via `exec()`

### Checklist — funções SQL (PL/pgSQL)

Também precisam ser criadas no fallback Kotlin via `exec()`.

## Ordem de implementação

| Tipo de solicitação | Ordem das camadas |
|---------------------|-------------------|
| Novo domínio completo | arquitetura → banco → backend → frontend → i18n |
| Nova tabela/schema | arquitetura → banco → backend |
| Novo endpoint/serviço | arquitetura → backend |
| Alteração de schema | banco → backend |
| Novo componente/página | frontend → i18n |
| Bug backend | backend |
| Bug banco | banco → backend |

**Regra geral:** features cross-layer seguem arquitetura → banco → backend → frontend → infra.

## Checklist obrigatório por feature

1. **i18n** — textos nos 3 locales (pt, en, es); backend com `?locale=` quando aplicável
2. **Responsividade** — mobile, tablet e desktop; testar em 320px
3. **Tema** — light e dark mode funcionando
4. **Tipos** — TypeScript sem `any`, interfaces em `types/index.ts`
5. **Loading/Error** — estados de carregamento e erro tratados

## Manutenção de regras

Após implementações que alterem estrutura/convenções, atualizar `.cursor/rules/`:

```bash
python scripts/update_rules.py
```

Revisar manualmente: descrições de novas migrations, conceitos de domínio, padrões novos.

## Conceitos de domínio

- **GA-ID**: identificador Gregory-Aland de manuscritos
- **Cobertura acumulativa**: século N inclui anteriores
- **Tipos**: papyrus, uncial, minuscule, lectionary
- **NTVMR**: API do INTF Münster para dados de manuscritos
- **TextualTopic**: MANUSCRIPTS, AUTOGRAPHS, APOCRYPHA, CANON, TEXTUAL_VARIANTS, TRANSLATION, CORRUPTION, SCRIPTURE_AUTHORITY
- **pg_trgm**: busca fuzzy com trigram similarity
- **Observatory**: observabilidade de visitantes (sessões, page views, fingerprinting)
- **Particionamento mensal**: `visitor_sessions` e `page_views` por RANGE em `created_at`
- **Google SSO + RBAC**: UserRole.ADMIN (acesso total) / MEMBER (páginas públicas)
- **AuthGate**: componente React que protege páginas admin
- **Dating enrichment**: datação por ano com DatingConfidence (HIGH/MEDIUM/LOW)
- **Church Councils**: concílios até 1000 d.C., heresias, cânones, vínculos patrísticos
- **Source Consensus Engine**: consolidação de claims por peso/score/fallback determinístico

## Idioma

- Código-fonte, variáveis e classes: **inglês**
- Comunicação com o usuário: **português**
