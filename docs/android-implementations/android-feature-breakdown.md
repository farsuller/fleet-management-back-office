# Android Feature Breakdown (Driver App + Customer App)

## Scope
This document defines the split between:
- Driver Android app capabilities
- Customer Android app capabilities

It is aligned with existing backend contracts and consolidated Android implementation docs.

## Source of Truth
- `docs/frontend-implementations/android-implementations/android-driver-app.md`
- `docs/frontend-implementations/android-implementations/android-customer-app.md`
- `docs/frontend-implementations/kotlin-multiplatform-prompt.md`
- `src/main/kotlin/com/solodev/fleet/modules/tracking/infrastructure/http/TrackingRoutes.kt`

## Persona Split

| Area | Driver App | Customer App |
|------|------------|--------------|
| Primary goal | High-frequency telemetry and duty compliance | Booking, active trip visibility, payments |
| Data pattern | Write-heavy + continuous | Read-heavy + transactional |
| Platform services | Foreground service, sensors, WorkManager, geofencing | Notification-driven UX, periodic tracking reads |
| Tracking transport | POST location + optional WS | Poll state/history (WS optional only if needed) |

## Driver Feature Set (Backend-Aligned)

### Core Driver Flows
- Authentication and role dispatch
- Shift-aware telemetry start/stop
- GPS + sensor capture
- Coordinate transmission and retry
- Live state visualization and operational status

### Driver Endpoints
- `GET /v1/tracking/routes/active`
- `POST /v1/tracking/vehicles/{id}/location`
- `GET /v1/tracking/vehicles/{vehicleId}/state`
- `GET /v1/tracking/vehicles/{vehicleId}/history`
- `WS /v1/fleet/live`

### Driver Non-Functional Requirements
- Rate-limit handling (`429`)
- Outage handling (`503` and offline)
- Idempotency headers for retry-safe write paths
- Work-hours/geofence privacy controls
- Storage retention and queue caps

## Customer Feature Set (Backend-Aligned)

### Core Customer Flows
- Account registration/login/profile
- Vehicle browsing and selection
- Rental booking lifecycle
- Active-trip tracking visibility
- Payment and payment-history view

### Customer Endpoints
- `POST /v1/users/register`
- `POST /v1/users/login`
- `GET/PATCH /v1/users/{id}`
- `GET /v1/vehicles`, `GET /v1/vehicles/{id}`
- `GET/POST /v1/rentals`, `GET /v1/rentals/{id}`
- `POST /v1/rentals/{id}/activate|complete|cancel`
- `GET /v1/tracking/vehicles/{vehicleId}/state`
- `GET /v1/tracking/vehicles/{vehicleId}/history`
- `POST /v1/accounting/invoices/{id}/pay`
- `GET /v1/accounting/payments/customer/{id}`

### Customer Non-Functional Requirements
- Idempotent payment submission
- Consistent API envelope parsing (`ApiResponse<T>`)
- Graceful stale/no-tracking states
- Reliable pagination handling for history views

## Delivery Model in KMP Repository

### Shared (`commonMain`)
- DTOs, API envelope models, repositories, use cases, validation

### Android-specific (`androidMain`)
- Driver-only: Foreground Service, sensors, WorkManager, geofence, Room queue
- Customer-only: app UI flow, local cache, notification integration

## Dependency Consolidation (Driver + Customer)

Use this as the single dependency ownership map for both apps.

| Dependency / Tooling | Driver App (KMP repo) | Customer App (separate repo) | Source Set / Module Guidance |
|---|---|---|---|
| **Ktor Client** | Required | Required | Put API contracts/serializers in `commonMain`; use platform engine wiring in `androidMain` |
| **Koin (DI)** | Recommended standard | Recommended standard | Prefer Koin modules per feature (`feature:*:di`) and app-level composition in `:app:*` |
| **Room** | Required (offline telemetry queue) | Required (catalog/rental history cache) | Keep DAO/entity/cache policy in `androidMain` and `core:database` |
| **Caching Logic** | Write-heavy queue + retry replay | Read-heavy cache + invalidation | Driver: queue semantics (ordered replay). Customer: cache-first reads with stale markers |
| **Coil** | Optional (maps/status assets) | Required (vehicle/media-rich UI) | Put image loaders/adapters in `core:ui` or `feature:*:presentation` |
| **Icons (Extended)** | Required | Required | Use vector drawable / Compose vector + Material Symbols Extended in `core:ui` |
| **JUnit (unit tests)** | Required | Required | `commonTest` for shared use cases + `androidUnitTest` for app-specific logic |
| **Instrumentation tests** | Required (service, sensors, WorkManager) | Required (critical user journeys) | `androidInstrumentedTest` per app repo |
| **Coroutines + Flow** | Required | Required | Shared state/use cases in `commonMain`, lifecycle collection in presentation layer |
| **Serialization (`kotlinx`)** | Required | Required | `commonMain` DTO and envelope parsing (`ApiResponse<T>`) |

### Consolidated Dependency Baseline

Driver app baseline:
- Ktor + Kotlinx serialization
- Koin DI
- Room + WorkManager + connectivity monitor
- Sensor/location stack
- JUnit + instrumentation tests
- Vector icon system

Customer app baseline:
- Ktor + Kotlinx serialization
- Koin DI
- Room cache for catalog/rental/tracking history
- Coil for media-heavy UI
- JUnit + instrumentation tests
- Vector icon system

### Modular Placement Rules

1. `commonMain`:
- API DTOs and `ApiResponse<T>` envelope models
- repository interfaces and use-case contracts
- validation and error mappers

2. `androidMain`:
- Room DAOs/entities and cache policy
- DI bootstrapping and Android bindings
- Coil image pipeline, notification handlers, permissions

3. `feature:*` modules:
- feature-specific ViewModel/state/events
- feature use cases (domain) and adapters (data)

4. `core:*` modules:
- reusable network/database/ui/navigation building blocks

### Testing Consolidation Matrix

| Test Layer | Driver App | Customer App |
|---|---|---|
| Unit tests | telemetry batching, retry/idempotency, policy rules | booking/payment state reducers, cache invalidation, pagination |
| Integration tests | tracking endpoint adapters, queue replay, websocket reconnection | rentals/payments adapters, tracking poll behavior |
| Instrumentation tests | foreground service lifecycle, permission flow, WorkManager execution | auth/navigation flow, booking-to-payment critical path |

### Icon and Asset Strategy

- Keep icon ownership in `core:ui`.
- Prefer SVG/vector assets over large PNGs.
- Use semantic naming (`ic_tracking_active`, `ic_rental_reserved`, `ic_payment_success`).
- Share tokenized sizing/color rules to avoid design drift across driver/customer apps.

## Consolidation Outcome
- Driver-specific consolidated plan lives in `android-driver-app.md`.
- Customer-specific consolidated plan lives in `android-customer-app.md`.
- This file is the high-level feature map for both apps.
