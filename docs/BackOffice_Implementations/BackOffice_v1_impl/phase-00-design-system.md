# Phase 0 — Design System Bootstrap

> **Status**: `COMPLETE`
> **Prerequisite**: —
> [← Back to master plan](../web-backoffice-implementation-plan.md)

**Goal**: establish all design tokens, conventions, and shared Compose components before any screen work begins.

---

## Deliverables

- [ ] `FleetTheme.kt` — MaterialTheme override with color tokens, typography scale, shape tokens
- [ ] `StatusBadge.kt` — reusable pill for all 14 status/priority variants
- [ ] `LoadingSkeleton.kt` — shimmer placeholder for every async panel
- [ ] `ConfirmDialog.kt` — modal with title, message, confirm/cancel actions
- [ ] `KpiCard.kt` — bento-style summary card: icon + value + label + optional sparkline
- [ ] `PaginatedTable.kt` — generic sortable/filterable table with load-more

## Design Token Checklist

- [ ] All color tokens defined (`Primary #1E40AF`, `Accent #F59E0B`, `Surface #F8FAFC`, all status semantics)
- [ ] Status badge: color + label + icon for every variant — WCAG AA (no color-only signals)
- [ ] Typography scale: `heading-xl` (Inter 700 32px) → `caption` (Inter 400 12px) + `mono` (JetBrains Mono 13px)
- [ ] Hover transitions: `200ms ease` on all interactive elements
- [ ] Touch targets: all `≥ 44px` tall
- [ ] Focus ring: `2px solid #3B82F6` on keyboard focus
- [ ] Empty state component: illustration slot + CTA button

## Verification

- [ ] All components render in isolation with each status variant
- [ ] Keyboard Tab order is logical on each component
- [ ] No hardcoded hex values outside of `FleetTheme.kt`
