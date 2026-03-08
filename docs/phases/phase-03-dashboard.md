# Phase 3 — Dashboard Screen

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 2
> [← Back to master plan](../web-backoffice-implementation-plan.md)

**Goal**: real-data summary bento grid visible immediately on login.

---

## Layout

```
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Total       │ Active      │ Fleet Live  │ Scheduled   │
│ Vehicles    │ Rentals     │ Status      │ Maintenance │ ← KpiCard ×4
├─────────────┴─────────────┴─────────────┴─────────────┤
│                                                         │
│             Fleet Map Panel (SVG schematic)             │
│                                                         │
├─────────────────────────────┬───────────────────────────┤
│  Recent Rentals (last 5)    │  Recent Maintenance (last 5) │
└─────────────────────────────┴───────────────────────────┘
```

## Deliverables

- [x] `GetDashboardUseCase` — owns `DashboardStats` + `DashboardSnapshot` models; `supervisorScope` parallel loading of all 4 repositories
- [x] `DashboardViewModel(getDashboardUseCase)` — delegates entirely to the use case; exposes `UiState<DashboardSnapshot>`; **does not call any repository directly**
- [x] `isRefreshing: StateFlow<Boolean>` exposed so the UI can show a subtle indicator during background refresh
- [ ] **KPI: Total Vehicles** — `GET /v1/vehicles` count
- [ ] **KPI: Active Rentals** — `GET /v1/rentals?status=ACTIVE` count
- [ ] **KPI: Fleet Live Status** — `GET /v1/tracking/fleet/status` (`totalVehicles`, `activeVehicles`)
- [ ] **KPI: Scheduled Maintenance** — aggregated count of `status=SCHEDULED`
- [ ] **Fleet Map Panel** — `GET /v1/tracking/routes` + WS `/v1/fleet/live` deltas
- [ ] **Recent Rentals** — `GET /v1/rentals?limit=5`
- [ ] **Recent Maintenance** — maintenance list `?limit=5`
- [ ] `LoadingSkeleton` shown on each panel **only on cold first load** (warm cache resolves before a frame is painted)
- [ ] Per-panel error state (one failure does not crash others)

## Caching Behavior

The Dashboard uses the **stale-while-revalidate** strategy powered by the `InMemoryCache` layer in each repository (see Phase 1.10). There is no Room/SQLite dependency — all caching is in-process.

| Scenario | Skeleton shown? | `isRefreshing`? |
|---|---|---|
| Cold first load (no cache) | Yes — all panels | No |
| Warm revisit (within TTL) | No | No |
| Explicit refresh button | No | Yes — subtle indicator |
| Network error on refresh | No (old data retained) | No (error banner instead) |

- `refresh()` passes `forceRefresh = true` to all 4 repository calls, bypassing in-memory cache and writing fresh results back on success.
- A failed `forceRefresh` does **not** wipe the existing `UiState.Success`; an error banner is overlaid instead.

## Verification

- [ ] All 4 KPI cards show real counts (not hardcoded)
- [ ] Fleet map renders at least one route path from API
- [ ] Recent lists populate from real API data
- [ ] Any single API failure shows in-card error state without affecting other panels
- [ ] Navigating away and back to Dashboard shows **no skeleton** (repo cache warm)
- [ ] Explicit refresh shows `isRefreshing = true` briefly, then updates counts
- [ ] Performing a mutation (cancel rental) and clicking refresh shows updated active rental count
