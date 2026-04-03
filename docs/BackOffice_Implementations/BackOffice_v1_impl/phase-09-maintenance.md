# Phase 9 — Maintenance Module

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 8
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## 9.1 Maintenance List (`/maintenance`)

- [x] Table: Job # · Vehicle · Type badge · Priority badge · Status badge · Scheduled Date · Est. Cost
- [x] Filter: Priority, Type, Status, Vehicle
- [x] "Schedule Job" → `/maintenance/new`

## 9.2 Maintenance Detail (`/maintenance/:id`)

- [x] Job info card
- [x] Transition buttons (only when valid):

  | Status | Button | Action |
  |---|---|---|
  | SCHEDULED | Start Job | `POST /v1/maintenance/{id}/start` |
  | SCHEDULED | Cancel Job | `POST /v1/maintenance/{id}/cancel` |
  | IN_PROGRESS | Complete Job | `POST /v1/maintenance/{id}/complete` |

- [x] "Complete Job" form: `laborCost` + `partsCost` inputs (both must be positive numbers)
- [x] Cost summary card: labor + parts + total

## 9.3 Schedule Maintenance (`/maintenance/new`)

- [x] Fields: vehicle selector, job type, priority, scheduled date, estimated cost, description

## Caching Behavior

> `MaintenanceRepositoryImpl` · TTL **120 s** · see Phase 1.10.

| Action | Cache effect |
|---|---|
| Open Maintenance list (warm) | Returns from `listCache` — no network |
| Explicit refresh | `forceRefresh = true` on `getJobs` |
| Schedule / Complete job | `listCache.clear()` |
| Cancel job | `listCache.clear()` |
| Vehicle Detail → Maintenance tab | Uses same `MaintenanceRepository.getJobs` with vehicle-scoped key — benefits from same cache |

`MaintenanceViewModel` should expose `isRefreshing: StateFlow<Boolean>`.

## Use Cases (Clean Architecture — Phase 1.11)

> Create these in `domain/usecase/maintenance/MaintenanceUseCases.kt` **before** building `MaintenanceViewModel`. See Phase 1.11 for the full checklist.

- [x] `GetMaintenanceJobsUseCase(maintenanceRepository)`, `GetMaintenanceJobUseCase(maintenanceRepository)`
- [x] `ScheduleMaintenanceUseCase(maintenanceRepository)` — `POST /v1/maintenance`
- [x] `CompleteMaintenanceUseCase(maintenanceRepository)` — `POST /v1/maintenance/{id}/complete` with labor + parts cost
- [x] `CancelMaintenanceUseCase(maintenanceRepository)` — `POST /v1/maintenance/{id}/cancel`
- [x] Note: `GetVehicleMaintenanceUseCase` already implemented in Phase 4 (`domain/usecase/vehicle/VehicleUseCases.kt`) — reuse it for the Vehicle Detail → Maintenance tab
- [x] Register all new use cases in `UseCaseModule.kt`; inject into `MaintenanceViewModel`

## Verification

- [x] URGENT priority badge renders with `#EF4444`
- [x] Complete Job validates `laborCost` and `partsCost` are positive numbers
- [x] Cancel shows `ConfirmDialog`
- [x] Completing a job and navigating back to list shows COMPLETED status (cache invalidated)
- [x] No skeleton flash on list revisit within 120 s
