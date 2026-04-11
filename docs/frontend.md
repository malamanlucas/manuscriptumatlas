# Frontend — Next.js / React / TypeScript

## Conventions

- **Server Components** by default; `"use client"` only when needed (hooks, context, interactivity)
- **Data fetching:** TanStack Query (`useQuery`, `useMutation`), functions in `lib/api/`
- **Types:** Explicit TypeScript, avoid `any`, interfaces in `types/`
- **Styling:** Tailwind utility-first, dark mode via `dark:` prefix (next-themes)
- **Responsiveness:** Mobile (320px+), tablet, desktop
  - Dashboards: `max-w-7xl mx-auto w-full`
  - Grids: `grid-cols-1 md:grid-cols-2 lg:grid-cols-3`
  - Tables: `overflow-x-auto`
  - Text: `text-sm md:text-base`, `p-4 md:p-6`
- **i18n:** `messages/{locale}.json` + `useTranslations()`. Zero hardcoded text.
- **State:** React Context (auth, sidebar), TanStack Query (server state), localStorage (token, theme)

## Pages (24 directories)

### Analysis & Visualization
| Route | Page | Description |
|-------|------|-------------|
| `/dashboard` | Dashboard | Coverage overview with century slider, stats cards, book coverage |
| `/manuscripts` | Manuscript Explorer | Filterable list (type, century, year range) |
| `/manuscripts/[gaId]` | Manuscript Detail | Individual manuscript with books, verses, dating |
| `/verse-lookup` | Verse Lookup | Find manuscripts containing a specific verse |
| `/timeline` | Timeline | Coverage evolution by century (line/area chart) |
| `/compare` | Compare | Manuscript type comparison (bar chart) |
| `/metrics` | NT Metrics | Stabilization, fragmentation, growth rates |
| `/metrics/[book]` | Book Metrics | Per-book metrics detail |

### Patristic
| Route | Page | Description |
|-------|------|-------------|
| `/patristic-dashboard` | Patristic Dashboard | Summary stats, charts by century/tradition |
| `/fathers` | Church Fathers | Filterable list (century, tradition, year range, search) |
| `/fathers/[id]` | Father Detail | Biography, dating, statements, councils |
| `/fathers/testimony` | Testimony | Textual statements with topic/century/tradition filters |

### Councils & Heresies
| Route | Page | Description |
|-------|------|-------------|
| `/councils` | Councils | List + Leaflet map view (filterable) |
| `/councils/[slug]` | Council Detail | Summary, canons, fathers, heresies, source provenance |
| `/heresies` | Heresies | List of condemned heresies |
| `/heresies/[slug]` | Heresy Detail | Description, related councils |

### Bible
| Route | Page | Description |
|-------|------|-------------|
| `/bible` | Bible Reader | Multi-translation reader with book/chapter navigation |
| `/bible/compare` | Compare | Side-by-side 2-4 translation comparison |
| `/bible/search` | Search | Full-text search with filters (version, testament, mode) |
| `/bible/interlinear` | Interlinear | Greek/Hebrew word-by-word with Strong's links |
| `/bible/strongs/[number]` | Strong's | Concordance entry with occurrences |

### Reference
| Route | Page | Description |
|-------|------|-------------|
| `/manuscript-count` | Manuscript Count | Count by type (papyrus, uncial, minuscule, lectionary) |
| `/history` | History | Historical documentation |
| `/sources` | Sources | Academic source information |
| `/methodology` | Methodology | Methodology documentation |
| `/faq` | FAQ | Frequently asked questions |
| `/wiki-llm` | LLM Architecture | LLM system documentation |

### Apologetics
| Route | Page | Description |
|-------|------|-------------|
| `/apologetics` | Apologetics | Topics and AI-generated responses |

### Admin (requires ADMIN role)
| Route | Page | Description |
|-------|------|-------------|
| `/observatory` | Observatory | Visitor analytics (sessions, pageviews, heatmaps) |
| `/ingestion-status` | Ingestion | Pipeline status + controls (run/reset per domain) |
| `/llm-usage` | LLM Usage | Provider stats, recent calls, rate limits |
| `/admin/users` | Users | User management (CRUD, role assignment) |

## Components (47)

### `bible/` (9)
- `BibleReader` — Chapter text + verse display + translation selector
- `BibleNavigation` — Book/chapter/verse dropdown (NT/OT separated)
- `VerseCompare` — Side-by-side multi-translation comparison
- `InterlinearView` — Greek/Hebrew word-by-word display
- `StrongsConcordance` — Strong's number concordance viewer
- `StrongsEntry` — Individual Strong's entry detail card
- `BibleSearchBar` / `BibleSearchResults` — Full-text search
- `LinkedVerseReader` — Verse reader with cross-references

### `charts/` (5)
- `TimelineChart` — Coverage by century (Recharts line/area)
- `ComparisonChart` — Manuscript type comparison (Recharts bar)
- `CouncilsTimelineChart` — Councils by century/type
- `FathersTimelineChart` — Fathers by century/tradition
- `Heatmap` — Activity heatmap (day x hour)

### `councils/` (4)
- `CouncilMapView` — Leaflet map with council markers
- `CouncilTypeBadge` — ECUMENICAL / REGIONAL / LOCAL badge
- `SourceProvenancePanel` — Multi-source claims + consensus display
- `SummaryToggle` — Expandable council summary

### `coverage/` (4)
- `BookCard` — Coverage card with color coding
- `CenturySlider` — Interactive century selector
- `GospelPanel` — Gospels aggregated coverage
- `VerseGrid` — Verse-by-verse coverage grid per chapter

### `ingestion/` (5) — Reusable pattern
- `IngestionPhasePanel` — Generic: status, progress bar, run/reset buttons
- `ManuscriptIngestionPanel` — Manuscripts (4 phases)
- `PatristicIngestionPanel` — Patristic (6 phases)
- `CouncilIngestionPanel` — Councils (14 phases)
- `BibleIngestionPanel` — Bible (25 phases)

### `layout/` (5)
- `Sidebar` — Collapsible navigation (analysis, patristic, bible, reference, admin)
- `SidebarContext` — React Context for sidebar state
- `Header` — Top navigation bar
- `LanguageSelector` — Locale picker (en/pt/es)
- `ThemeToggle` — Dark/light/system mode toggle

### `observatory/` (7)
- `AuthGate` — Login gate + ADMIN role check (fullscreen blocking)
- `OverviewTab` / `ExplorerTab` / `VisitorsTab` / `TimelineTab` — Analytics tabs
- `SessionDrawer` — Session detail view
- `VisitorTracker` — Invisible fingerprinting + session tracking

### `llm/` (3)
- `LlmProviderCard` — Provider stats (Anthropic/OpenAI)
- `LlmRateLimitPanel` — Rate limit status
- `LlmUsageTable` — Recent LLM calls table

## Hooks (18 files, 80+ exports)

All hooks wrap TanStack Query (`useQuery` / `useMutation`) with auto-refetch where appropriate.

| Hook File | Domain | Key Exports |
|-----------|--------|-------------|
| `useAuth` | Auth | user, login, logout, isAdmin |
| `useManuscripts` | Manuscripts | useManuscripts, useManuscriptDetail |
| `useCoverage` | Coverage | useCoverageByCentury, useChapterCoverage, useGospelCoverage |
| `useMissing` | Coverage | useMissingVerses |
| `useStats` | Statistics | useStatsOverview |
| `useTimeline` | Timeline | useTimeline (book/type filters) |
| `useMetrics` | Metrics | useNtMetrics, useBookMetrics, useManuscriptsCount |
| `useChurchFathers` | Patristic | useChurchFathers, useChurchFatherDetail, useSearchChurchFathers, usePatristicStats |
| `useTextualStatements` | Patristic | useTextualStatements, useFatherStatements, useSearchStatements, useTopicsSummary |
| `useCouncils` | Councils | useCouncils, useCouncilDetail, useSearchCouncils, useCouncilMapPoints |
| `useCouncilIngestion` | Councils | useCouncilIngestionPhases, useRunCouncilPhase |
| `useHeresies` | Heresies | useHeresies, useHeresyDetail, useHeresyCouncils |
| `useBible` | Bible | useBibleVersions/Books/Chapter, useBibleInterlinear, useStrongsConcordance, useLexiconEntry |
| `useApologetics` | Apologetics | useApologeticTopics, useApologeticTopicDetail |
| `useIngestion` | Ingestion | useIngestionStatus, useTriggerIngestion, useResetDomain, useDatingEnrichment |
| `useIngestionPhases` | Ingestion | useManuscript/Patristic/Council/BibleIngestionPhases, useRunPhase, useRunAllPhases |
| `useVisitorAnalytics` | Observatory | useAnalyticsOverview/Live/Sessions/Visitors/Heatmap/TopPages/Trends |
| `useLlmUsage` | LLM | useLlmDashboard, useLlmRateLimits |

## API Functions (130+ across 16 files in `lib/api/`)

| File | Domain | Functions |
|------|--------|-----------|
| `client.ts` | Core | fetchJson, fetchJsonAuth, getAuthToken, setAuthToken, clearAuthToken |
| `index.ts` | Re-exports | Barrel file for all API modules |
| `auth.ts` | Auth | loginWithGoogle, getAuthMe, getUsers, createUser, updateUserRole, deleteUser |
| `coverage.ts` | Coverage | getCoverage, getCoverageByCentury, getChapterCoverage, getGospelCoverage, getMissingVerses |
| `timeline.ts` | Timeline | getTimeline, getTimelineFull |
| `manuscripts.ts` | Manuscripts | getManuscripts, getManuscriptDetail, getManuscriptsForVerse |
| `metrics.ts` | Metrics | getNtMetrics, getBookMetrics, getManuscriptsCount, getStatsOverview |
| `fathers.ts` | Patristic | getChurchFathers, getChurchFatherDetail, searchChurchFathers, getFatherStatements, getTopicsSummary, getPatristicStats |
| `councils.ts` | Councils | getCouncils, searchCouncils, getCouncilDetail, getCouncilFathers/Canons/Heresies/Sources, getCouncilMapPoints |
| `heresies.ts` | Heresies | getHeresies, getHeresyDetail, getHeresyCouncils |
| `sources.ts` | Sources | getSources, getSourceDetail |
| `bible.ts` | Bible | getBibleVersions/Books/Chapter/Verse, getBibleInterlinear, getLexiconEntry, compareBible, searchBible |
| `apologetics.ts` | Apologetics | getApologeticTopics, getApologeticTopicDetail, createApologeticResponse |
| `ingestion.ts` | Ingestion | 23 functions (status, trigger, reset, per-domain phase control) |
| `analytics.ts` | Observatory | 17 functions (overview, sessions, visitors, heatmap, trends) |
| `llm.ts` | LLM | fetchLlmUsageDashboard, fetchLlmUsageLogs, fetchLlmRateLimits |

## Types (75+ across 14 files in `types/`)

Organized by domain: `index.ts`, `common.ts`, `coverage.ts`, `manuscripts.ts`, `metrics.ts`, `fathers.ts`, `councils.ts`, `heresies.ts`, `ingestion.ts`, `analytics.ts`, `auth.ts`, `llm.ts`, `bible.ts`, `apologetics.ts`.

Key types: `ManuscriptSummary`, `ChurchFatherDetail`, `CouncilDetailDTO`, `TextualStatementDTO`, `PhaseStatusDTO`, `BibleChapterResponse`, `InterlinearWordDTO`, `LexiconEntryDTO`, `AnalyticsOverview`, `LlmUsageDashboardDTO`.

## i18n — 3 Locales, 44 Namespaces

**Locales:** en, pt, es (always update all 3 simultaneously)

**Setup:** `next-intl` with locale-prefixed routing (`/pt/dashboard`, `/en/dashboard`)

**Key namespaces:** auth, bible, councils, dashboard, fathers, heresies, ingestion, llmUsage, manuscripts, methodology, observatory, sidebar, stats, timeline, verseLookup, wikiLlm.

## Layout Hierarchy

```
app/layout.tsx (root HTML)
  └── app/[locale]/layout.tsx (NextIntlClientProvider)
        └── Providers (Google OAuth, Theme, Auth, QueryClient)
              └── VisitorTracker (invisible fingerprinting)
                    └── app/[locale]/(atlas)/layout.tsx
                          ├── SidebarProvider (state context)
                          ├── Sidebar (left nav, collapsible sections)
                          └── main (content area, pl-64 on desktop)
```

## Theme

- **Engine:** next-themes with class-based strategy
- **Modes:** Light, Dark, System (auto-detect)
- **CSS Variables:** Defined in `globals.css` (`:root` for light, `.dark` for dark)
- **Colors:** Blue-dominant palette — primary `#2563eb` (light) / `#3b82f6` (dark)
