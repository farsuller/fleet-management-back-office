# Phase 4 — Vehicles Module

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 3
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## 4.1 Vehicles List (`/vehicles`)

- [x] Table columns: License Plate · Make/Model · Year · State badge · Mileage (km)
- [x] Filter bar: State filter chips (null + all VehicleState values)
- [x] "Load More" pagination — loads up to 100 items per fetch; no cursor-based lazy loading
- [x] "Add Vehicle" button visible to ADMIN / FLEET_MANAGER only

## 4.2 Vehicle Detail (`/vehicles/:id`) — 5 Tabs

- [x] **Info tab** — all fields; inline edit form for ADMIN/FLEET_MANAGER (`GET/PATCH /v1/vehicles/{id}`)
- [x] **State tab** — current state badge + valid transition buttons only

  | From | Button | Endpoint |
  |---|---|---|
  | AVAILABLE | Send to Maintenance | `PATCH state=MAINTENANCE` |
  | AVAILABLE | Retire Vehicle | `PATCH state=RETIRED` |
  | MAINTENANCE | Mark Available | `PATCH state=AVAILABLE` |
  | RENTED | (no manual transitions) | — |

- [x] **Odometer tab** — history list + "Record Reading" form (validates new > last)
- [x] **Maintenance tab** — all jobs for this vehicle with status/priority badges (`GET /v1/maintenance/vehicle/{id}`)
- [x] **Tracking History tab** — paginated location list: timestamp, lat/lon, speed, heading (`GET /v1/tracking/vehicles/{id}/history`)

## 4.3 Create Vehicle (`/vehicles/new`)

- [x] Fields: VIN (17 chars, mono font), plate, make, model, year, color
- [x] Inline validation on blur for all fields
- [x] VIN rejects non-17-char input before HTTP call

## Caching Behavior

> Powered by `VehicleRepositoryImpl` · TTL **120 s** · see Phase 1.10 for full rules.

| Action | Cache effect |
|---|---|
| Open Vehicles list (warm) | Returns from cache — no network request |
| Open Vehicles list (expired) | Fetches API, replaces cache entry |
| Pull-to-refresh / explicit refresh | `forceRefresh = true` — bypasses cache, updates entry on success |
| Create / Update / Delete / State change | `listCache.clear()` — next list fetch goes to network |
| Odometer record | No cache clear (individual reading, not list query) |
| Vehicle Detail tab (Maintenance / Tracking History) | `MaintenanceRepository` (120 s) and `TrackingRepository` (history uncached — raw reads) used directly |

`VehiclesViewModel` should expose `isRefreshing: StateFlow<Boolean>` identical to `DashboardViewModel` so the UI shows a top-bar progress indicator during explicit refresh without re-showing the skeleton table.

## Use Cases (Clean Architecture — Phase 1.11)

> `VehiclesViewModel` injects **9 use cases** from `domain/usecase/vehicle/VehicleUseCases.kt`. No repository is injected directly into the ViewModel.

| Use Case | Delegates to |
|---|---|
| `GetVehiclesUseCase` | `vehicleRepository.getVehicles(cursor, limit, state, forceRefresh)` |
| `GetVehicleUseCase` | `vehicleRepository.getVehicle(id)` |
| `CreateVehicleUseCase` | `vehicleRepository.createVehicle(request)` |
| `UpdateVehicleUseCase` | `vehicleRepository.updateVehicle(id, request)` |
| `UpdateVehicleStateUseCase` | `vehicleRepository.updateVehicleState(id, state)` |
| `UpdateOdometerUseCase` | `vehicleRepository.updateOdometer(id, request)` |
| `DeleteVehicleUseCase` | `vehicleRepository.deleteVehicle(id)` |
| `GetVehicleMaintenanceUseCase` | `maintenanceRepository.getJobsByVehicle(vehicleId)` |
| `GetVehicleLocationHistoryUseCase` | `trackingRepository.getVehicleHistory(vehicleId, ...)` |

## Verification

- [x] Vehicle list loads paginated data from API
- [x] State badge colors match design tokens exactly
- [x] Invalid state transitions are never offered as buttons
- [x] VIN field rejects < 17 or > 17 characters
- [x] Odometer rejects new reading ≤ last reading
- [x] Navigating away and back to Vehicles shows no skeleton (cache warm)
- [x] State change followed by list revisit shows updated badge (cache invalidated)
