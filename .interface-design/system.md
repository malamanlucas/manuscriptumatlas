# Design System — Manuscriptum Atlas

## Direction

**Personality:** Scholarly Precision — academic research tool with data density
**Foundation:** Cool slate palette (light: slate-50/white, dark: slate-900/950)
**Depth:** Borders + subtle surface elevation (cards with `border-border bg-card`)
**Feel:** Dense like a research terminal, organized like a library catalog

## Context

**Who:** Researchers, theologians, academics analyzing NT manuscript coverage, church fathers, and councils.
**What they do:** Compare manuscript data across centuries, explore patristic testimony, track ingestion pipelines, monitor LLM costs.
**Feel:** Precise, trustworthy, information-dense. Not playful — scholarly. Dark mode is primary usage.

## Tokens

### Colors (CSS Custom Properties)
```
Light:
  --background: #f8fafc (slate-50)
  --foreground: #0f172a (slate-900)
  --card: #ffffff
  --primary: #1e40af (blue-800)
  --secondary: #f1f5f9 (slate-100)
  --muted-foreground: #64748b (slate-500)
  --border: #e2e8f0 (slate-200)
  --sidebar: #1e293b (slate-800)

Dark:
  --background: #0f172a (slate-900)
  --foreground: #f1f5f9 (slate-100)
  --card: #1e293b (slate-800)
  --primary: #3b82f6 (blue-500)
  --secondary: #334155 (slate-700)
  --muted-foreground: #94a3b8 (slate-400)
  --border: #334155 (slate-700)
  --sidebar: #020617 (slate-950)
```

### Semantic Color Scale (Tailwind)
- Success: emerald-500/600 (coverage, success states)
- Warning: amber-500/600 (partial coverage, caution)
- Error: red-500/600 (failures, missing data)
- Info: blue-500/600 (primary actions, highlights)
- Purple: purple-500/600 (secondary metrics)

### Spacing
Base: 4px (Tailwind default)
Common: p-4 (16px), p-5 (20px), p-6 (24px), gap-3 (12px), gap-4 (16px)

### Radius
- Cards/Panels: rounded-xl (12px)
- Buttons/Badges: rounded-lg (8px)
- Progress bars: rounded-full
- Icon backgrounds: rounded-lg (8px)

### Typography
- Font: system-ui stack (native performance)
- Mono: ui-monospace, SF Mono (for data/numbers)
- Headings: text-base font-semibold (section), text-lg/xl font-bold (page)
- Body: text-sm (14px default)
- Labels: text-xs text-muted-foreground
- Data values: text-2xl font-bold (stat cards), tabular-nums (tables)

## Patterns

### Stat Card (Dashboard)
```
rounded-xl border border-border bg-card p-5
  flex items-center gap-3
    icon-container: rounded-lg p-2 {color}-100 dark:{color}-900
      icon: h-5 w-5 text-{color}-600 dark:text-{color}-400
    text:
      value: text-2xl font-bold
      label: text-xs text-muted-foreground
```

### Section Card
```
rounded-xl border border-border bg-card p-4 md:p-5
  heading: text-base font-semibold flex items-center gap-2 mb-3/4
```

### Page Layout (Dashboard/Admin)
```
max-w-7xl mx-auto w-full p-4 md:p-6 space-y-6
```

### Responsive Grids
- Stats: grid-cols-1 sm:grid-cols-2 md:grid-cols-4
- Cards: grid-cols-1 md:grid-cols-2
- Book grid: grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5
- Topic badges: grid-cols-1 sm:grid-cols-2 lg:grid-cols-4

### Buttons
- Primary: bg-primary text-primary-foreground rounded-lg px-3 py-1.5 text-sm font-medium
- Secondary: bg-secondary text-secondary-foreground rounded-lg px-3 py-1.5 text-sm font-medium
- Period selector: bg-muted p-1 group with active = bg-background shadow-sm

### Table
```
rounded-xl border border-border bg-card overflow-x-auto
  thead: border-b text-xs text-muted-foreground font-medium px-3 py-2.5
  tbody: divide-y divide-border
  tr: hover:bg-muted/30 transition-colors
  td: px-3 py-2 text-sm
  numbers: tabular-nums, text-right
```

### Progress Bar
```
container: h-2 w-full rounded-full bg-muted
  fill: h-2 rounded-full {color} transition-all
```

### Horizontal Bar Chart
```
flex items-center gap-3
  label: w-8 text-right text-xs font-medium text-muted-foreground
  bar-container: h-5 flex-1 rounded bg-muted
    bar: h-5 rounded bg-blue-500/80 px-1.5 text-[10px] font-medium text-white
```

### Error State
```
rounded-xl border border-red-300 bg-red-50 p-4/6 text-red-700
  dark:border-red-800 dark:bg-red-950 dark:text-red-300
```

### Loading State
```
skeleton: animate-pulse rounded-xl border border-border bg-card p-5/6
  content: h-8 w-20/24 rounded bg-secondary
spinner: flex items-center justify-center py-12/16
  Loader2 h-6 w-6 animate-spin text-muted-foreground
```

### Sidebar
- Collapsible sections with ChevronDown rotation animation
- Active item: bg-white/10 text-white
- Inactive: text-white/60-70 hover:bg-white/5
- Section labels: text-xs font-semibold uppercase tracking-wider text-white/40

### Status Indicators
- Dot: h-2.5 w-2.5 rounded-full {color}
- Badge: rounded-md px-2 py-1 text-xs font-medium {bg} {text}
- Coverage colors: >=90% emerald, 60-90% emerald-400, 30-60% amber, <30% red

## Decisions

| Decision | Rationale | Date |
|----------|-----------|------|
| Slate palette | Academic, scholarly feel — not flashy | 2026-03-14 |
| Borders over shadows | Information density, cleaner at small sizes | 2026-03-14 |
| System fonts | Performance, native feel across platforms | 2026-03-14 |
| rounded-xl cards | Softer than sharp corners but not bubbly | 2026-03-14 |
| Dark mode primary | Researchers work long hours, easier on eyes | 2026-03-14 |
| Semantic color icons | Blue=info, emerald=success, amber=warning, purple=metrics | 2026-03-14 |
| text-2xl stat values | Scannable at a glance in dashboard cards | 2026-03-14 |
| Collapsible sidebar | 30+ nav items need organization, saves vertical space | 2026-03-14 |
