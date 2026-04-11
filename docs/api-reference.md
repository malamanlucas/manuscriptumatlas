# API Reference — REST Endpoints

Base URL: `http://localhost:8080` (backend) or `http://localhost:3000/api` (frontend proxy)

All endpoints accept `?locale=pt|en|es` for translated content where applicable.

## Public Endpoints

### Coverage
```
GET /coverage                              — Full coverage report (?type=papyrus&century=4)
GET /coverage/{book}                       — Coverage for a specific book
GET /coverage/{book}/chapters/{century}    — Chapter-level coverage (?type=)
GET /coverage/gospels/{century}            — Gospel coverage aggregate (?type=)
GET /century/{number}                      — Cumulative coverage up to century N (?type=)
GET /timeline                              — Coverage evolution (?book=John&type=papyrus)
GET /timeline/full                         — Full NT timeline (?type=)
GET /missing/{book}/{century}              — Missing verses (?type=)
```

### Statistics & Metrics
```
GET /stats/overview                        — Global statistics (total, by type/century/book)
GET /stats/manuscripts-count               — Count by manuscript type
GET /metrics/nt                            — NT-wide academic metrics
GET /metrics/{book}                        — Book-level metrics
```

### Manuscripts
```
GET /manuscripts                           — Paginated list (?type=&century=&page=&limit=&yearMin=&yearMax=)
GET /manuscripts/{gaId}                    — Manuscript detail
GET /books                                 — All NT books with translations
GET /verses/manuscripts                    — Manuscripts for a verse (?book=&chapter=&verse=&type=)
```

### Church Fathers
```
GET /fathers                               — Paginated list (?century=&tradition=&page=&limit=&yearMin=&yearMax=)
GET /fathers/search                        — Fuzzy search (?q=&limit=)
GET /fathers/{id}                          — Father detail
GET /fathers/{id}/statements               — Statements by father
GET /fathers/{id}/councils                 — Councils where father participated
GET /fathers/statements                    — All statements (?topic=&century=&tradition=&page=&limit=)
GET /fathers/statements/search             — Search statements (?q=&limit=)
GET /fathers/statements/topics/summary     — Statement count by topic
GET /fathers/stats                         — Patristic statistics
```

### Councils
```
GET /councils                              — Paginated list (?century=&type=&yearMin=&yearMax=&page=&limit=)
GET /councils/search                       — Search (?q=&limit=)
GET /councils/types/summary                — Count by type (ECUMENICAL/REGIONAL/LOCAL)
GET /councils/map                          — Geographic data for Leaflet map
GET /councils/{slug}                       — Council detail (summary, consensus, sources)
GET /councils/{slug}/fathers               — Participating fathers
GET /councils/{slug}/canons                — Council canons (?page=&limit=)
GET /councils/{slug}/heresies              — Condemned heresies
GET /councils/{slug}/sources               — Source claims (provenance)
```

### Heresies
```
GET /heresies                              — Paginated list (?page=&limit=)
GET /heresies/{slug}                       — Heresy detail
GET /heresies/{slug}/councils              — Councils that addressed this heresy
GET /sources                               — All academic sources
```

### Bible
```
GET /bible/versions                        — Available translations (?testament=NT|OT|FULL)
GET /bible/books                           — All books (?testament=)
GET /bible/read/{version}/{book}/{chapter} — Chapter text
GET /bible/read/{version}/{book}/{chapter}/{verse} — Single verse
GET /bible/compare/{book}/{chapter}        — Chapter comparison (?versions=KJV,AA)
GET /bible/compare/{book}/{chapter}/{verse} — Verse comparison (?versions=)
GET /bible/interlinear/{book}/{chapter}    — Chapter interlinear (Greek/Hebrew)
GET /bible/interlinear/{book}/{chapter}/{verse} — Verse interlinear
GET /bible/strongs/{strongsNumber}         — Strong's concordance (?page=&limit=)
GET /bible/lexicon/{strongsNumber}         — Lexicon entry (?locale=)
GET /bible/search                          — Full-text search (?q=&version=&testament=&book=&page=&limit=)
GET /bible/ref/{reference...}              — Parse reference (?locale=) e.g., "John 3:16"
```

## Auth Endpoints

```
POST /auth/login                           — Google SSO (body: {credential: "google_id_token"})
POST /auth/dev-login                       — Dev-only admin JWT (localhost only)
GET  /auth/me                              — Current user info [JWT]
GET  /auth/role                            — Current user role [JWT]
```

## Admin Endpoints [JWT, ADMIN role]

### Global Ingestion
```
GET  /admin/ingestion/status               — Current ingestion status
POST /admin/ingestion/run                  — Trigger full ingestion
POST /admin/ingestion/reset                — Reset all + re-ingest
POST /admin/reset/{domain}                 — Reset domain (manuscripts|patristic|councils|bible)
POST /admin/enrich-dating                  — LLM dating enrichment (?domain=fathers&limit=50)
```

### Manuscript Ingestion
```
GET  /admin/manuscripts/ingestion/phases   — Phase statuses
POST /admin/manuscripts/ingestion/run/{phase} — Run single phase
POST /admin/manuscripts/ingestion/run-all  — Run all (skips completed)
```

### Patristic Ingestion
```
GET  /admin/patristic/ingestion/phases     — Phase statuses
POST /admin/patristic/seed                 — Seed fathers (?filter=clement_of_rome)
POST /admin/patristic/translate            — Translate (?force=true)
POST /admin/patristic/ingestion/run/{phase} — Run single phase (?filter=)
POST /admin/patristic/ingestion/run        — Run selected (body: {phases: [...]}, ?filter=)
POST /admin/patristic/ingestion/run-all    — Run all (skips completed, ?filter=)
```

### Council Ingestion
```
GET  /admin/councils/ingestion/phases      — Phase statuses
POST /admin/councils/ingestion/run/{phase} — Run single phase
POST /admin/councils/ingestion/run         — Run selected (body: {phases: [...]})
POST /admin/councils/ingestion/run-all     — Run all (skips completed)
GET  /admin/councils/ingestion/cache       — Source file cache stats
GET  /admin/councils/audit                 — Audit missing councils (?maxYear=&onlyMissing=)
```

### Bible Ingestion
```
GET  /admin/bible/ingestion/phases         — Phase statuses
POST /admin/bible/ingestion/run/{phase}    — Run single phase
POST /admin/bible/ingestion/run            — Run selected (body: {phases: [...]})
POST /admin/bible/ingestion/run-all        — Run all (skips completed)
POST /admin/bible/glosses/clear            — Clear all glosses + alignments + reset phases
POST /admin/bible/glosses/fix-corrupted    — Clear only corrupted PT glosses (JSON fragments)
```

### LLM Usage
```
GET  /admin/llm/usage                      — Dashboard metrics (?period=7d|30d|90d)
GET  /admin/llm/usage/logs                 — Recent calls (?limit=&provider=)
GET  /admin/llm/rate-limits                — Rate limiter status
```

### User Management
```
GET  /auth/users                           — List all users [JWT]
POST /auth/users                           — Create user (body: {email, displayName, role}) [JWT]
PATCH /auth/users/{id}/role                — Update role (body: {role}) [JWT]
DELETE /auth/users/{id}                    — Delete user [JWT]
```

### Visitor Analytics
```
POST /visitor/session/create               — Create session (public, fingerprinting)
POST /visitor/session/{sessionId}/heartbeat — Keep session alive (public)
POST /visitor/pageview                     — Log pageview (public)

GET  /admin/visitor/stats/daily            — Daily stats [JWT]
GET  /admin/visitor/stats/realtime         — Live visitors [JWT]
GET  /visitor/analytics/overview           — Overview metrics (?from=&to=&days=) [JWT]
GET  /visitor/analytics/live               — Real-time activity [JWT]
GET  /visitor/analytics/sessions           — Session list (filterable) [JWT]
GET  /visitor/analytics/sessions/{id}      — Session detail [JWT]
GET  /visitor/analytics/sessions/{id}/pageviews — Pageviews in session [JWT]
GET  /visitor/analytics/visitors           — Unique visitors [JWT]
GET  /visitor/analytics/visitors/{id}      — Visitor profile [JWT]
GET  /visitor/analytics/visitors/{id}/sessions — Visitor sessions [JWT]
GET  /visitor/analytics/timeline/sessions  — Sessions over time [JWT]
GET  /visitor/analytics/timeline/pageviews — Pageviews over time [JWT]
GET  /visitor/analytics/timeline/heatmap   — Activity heatmap [JWT]
GET  /visitor/analytics/top/pages          — Top pages [JWT]
GET  /visitor/analytics/top/referrers      — Top referrers [JWT]
GET  /visitor/analytics/distribution       — Distribution (?field=browser|os|device) [JWT]
GET  /visitor/analytics/trends             — Trends (?days=7) [JWT]
GET  /visitor/analytics/filters/values     — Available filter options [JWT]
```

## Response Format

All responses use JSON (`application/json`). Paginated endpoints return:
```json
{
  "total": 147,
  "page": 1,
  "limit": 20,
  "items": [...]
}
```

Error responses:
```json
{ "error": "Description of the error" }
```

## Authentication

Protected endpoints require `Authorization: Bearer <jwt_token>` header.
Token is obtained via `POST /auth/login` (Google SSO) or `POST /auth/dev-login` (dev only).
Token expires after 8 hours.
