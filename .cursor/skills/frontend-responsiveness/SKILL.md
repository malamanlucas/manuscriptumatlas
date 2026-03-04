---
name: frontend-responsiveness
description: Ensures responsive layouts in Next.js/Tailwind projects. Use when fixing layout issues, empty horizontal space, content not stretching, or when the user mentions responsividade, layout, or elements not filling the screen.
---

# Frontend Responsiveness

## Quick Fix: Content Not Stretching

When main content has empty space on the right (sidebar layout):

**Problem**: `max-w-3xl` (768px) or similar constrains width.

**Solution**: Use `max-w-7xl` (1280px) for dashboard/admin pages, with `w-full` and `mx-auto`:

```tsx
// Before - narrow, leaves empty space
<div className="p-4 md:p-6 space-y-6 max-w-3xl">

// After - fills available width up to 1280px, centered
<div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
```

## Max-Width Guidelines by Page Type

| Page Type | Recommended | Tailwind |
|-----------|-------------|----------|
| Dashboard, admin, ingestion | Wide | `max-w-7xl` (1280px) |
| Content-heavy (methodology, FAQ) | Medium | `max-w-3xl` or `max-w-5xl` |
| Forms, modals | Narrow | `max-w-sm` to `max-w-md` |

## Layout Structure (Atlas)

- `main` has `flex-1 pl-0 md:pl-64` (sidebar offset)
- Page content container should use `w-full` + `max-w-*` + `mx-auto` to utilize space
- Avoid `max-w-3xl` on dashboard-style pages with many cards/sections

## Breakpoints (Tailwind)

- `sm:` 640px
- `md:` 768px
- `lg:` 1024px
- `xl:` 1280px

## Checklist

- [ ] Content fills available width (no empty lateral space on desktop)
- [ ] Grid/flex adapts: `grid-cols-1 md:grid-cols-2 lg:grid-cols-4`
- [ ] Tables: `overflow-x-auto` on mobile
- [ ] Padding scales: `p-4 md:p-6`
