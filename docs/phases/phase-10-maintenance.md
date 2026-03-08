# Phase 10 — Maintenance Module

> **Status**: `NOT STARTED`
> **Prerequisite**: Phase 4
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## 10.1 Maintenance List (`/maintenance`)

- [ ] Table: Job # · Vehicle · Type badge · Priority badge · Status badge · Scheduled Date · Est. Cost
- [ ] Filter: Priority, Type, Status, Vehicle
- [ ] "Schedule Job" → `/maintenance/new`

## 10.2 Maintenance Detail (`/maintenance/:id`)

- [ ] Job info card
- [ ] Transition buttons (only when valid):

  | Status | Button | Action |
  |---|---|---|
  | SCHEDULED | Start Job | `POST /v1/maintenance/{id}/start` |
  | SCHEDULED | Cancel Job | `POST /v1/maintenance/{id}/cancel` |
  | IN_PROGRESS | Complete Job | `POST /v1/maintenance/{id}/complete` |

- [ ] "Complete Job" form: `laborCost` + `partsCost` inputs (both must be positive numbers)
- [ ] Cost summary card: labor + parts + total

## 10.3 Schedule Maintenance (`/maintenance/new`)

- [ ] Fields: vehicle selector, job type, priority, scheduled date, estimated cost, description

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

- [ ] `GetMaintenanceJobsUseCase(maintenanceRepository)`, `GetMaintenanceJobUseCase(maintenanceRepository)`
- [ ] `ScheduleMaintenanceUseCase(maintenanceRepository)` — `POST /v1/maintenance`
- [ ] `CompleteMaintenanceUseCase(maintenanceRepository)` — `POST /v1/maintenance/{id}/complete` with labor + parts cost
- [ ] `CancelMaintenanceUseCase(maintenanceRepository)` — `POST /v1/maintenance/{id}/cancel`
- [ ] Note: `GetVehicleMaintenanceUseCase` already implemented in Phase 4 (`domain/usecase/vehicle/VehicleUseCases.kt`) — reuse it for the Vehicle Detail → Maintenance tab
- [ ] Register all new use cases in `UseCaseModule.kt`; inject into `MaintenanceViewModel`

## Verification

- [ ] URGENT priority badge renders with `#EF4444`
- [ ] Complete Job validates `laborCost` and `partsCost` are positive numbers
- [ ] Cancel shows `ConfirmDialog`
- [ ] Completing a job and navigating back to list shows COMPLETED status (cache invalidated)
- [ ] No skeleton flash on list revisit within 120 s
