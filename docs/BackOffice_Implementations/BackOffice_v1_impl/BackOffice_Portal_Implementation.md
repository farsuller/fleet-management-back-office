# BackOffice Portal — Complete Implementation Reference

> **Status**: `COMPLETE` (Phases 00–10 All Delivered)
> **Platform**: Kotlin Multiplatform · Compose Multiplatform (Kotlin/Wasm)
> **Target**: `wasmJs` browser via `wasmJsBrowserDevelopmentRun` / `wasmJsBrowserProductionWebpack`
> **Last Updated**: 2026-04-03

---

## Tech Stack (Current Versions)

| Library / Tool | Version |
|---|---|
| Kotlin | `2.3.0` |
| Compose Multiplatform | `1.10.0` |
| KSP | `2.3.6` (standalone versioning) |
| Ktor Client | `3.1.2` |
| Koin | `4.0.2` |
| Coil | `3.1.0` |
| kotlinx.serialization | `1.8.0` |
| kotlinx.coroutines | `1.10.1` |
| kotlinx.datetime | `0.7.1` |
| KSafe | `1.7.0` |
| Charty | `3.0.0-rc01` |
| K2D (architecture) | `0.4.8` (KSP — commonMainMetadata only) |
| AGP | `8.11.2` |
| Material3 | `1.10.0-alpha05` |

> **Note**: The project targets `wasmJs` as the primary web output. All build tasks use `wasmJs*` variants.
> K2D compiler runs only on `kspCommonMainMetadata` — `kspWasmJs` and `kspAndroid` are intentionally excluded to avoid KSP processor crashes.

---

## Architecture Overview

```
Presentation  →  Domain (use cases)  →  Data (repositories)  →  Infrastructure (API client)
```

**Module load order** in `main.kt`:
```
storageModule → networkModule → trackingModule → repositoryModule → useCaseModule → viewModelModule
```

Each layer only depends on the layer directly below it. No ViewModel, no Compose, and no Ktor imports are allowed inside the domain layer.

---

## Phase 1 — Shared Core Infrastructure

### 1.1 API DTOs (`commonMain/api/dto/`)

- [x] `auth/` — `LoginRequest`, `LoginResponse`, `UserDto`
- [x] `vehicle/` — `VehicleDto`, `VehicleStateRequest`
- [x] `rental/` — `RentalDto`, `CreateRentalRequest`
- [x] `customer/` — `CustomerDto`
- [x] `maintenance/` — `MaintenanceJobDto`
- [x] `accounting/` — `InvoiceDto`, `PaymentDto`, `AccountDto`
- [x] `tracking/` — `VehicleStateDto`, `VehicleStateDelta`, `VehicleRouteState`, `RouteDto`
- [x] All monetary fields as `Long`; timestamps as `Long`
- [x] All enums use `@SerialName` + `UNKNOWN` fallback
- [x] **All response DTO fields are nullable (`? = null`)** — prevents deserialization crashes when backend omits optional fields

### 1.2 `ApiResponse<T>` + `ApiError`

- [x] `ApiResponse<T>` envelope with `success`, `data`, `error`, `requestId`
- [x] `ApiError` with `code`, `message`, `fieldErrors: List<FieldError>`

### 1.3 `FleetApiClient`

- [x] Ktor `HttpClient` configured with `ContentNegotiation` + JSON
- [x] `Authorization: Bearer` injected via `HttpClientPlugin` from `SecureStorage`
- [x] On `401` → clear token + emit `AuthEvent.Unauthorized` on `SharedFlow`
- [x] On `429` → emit `AuthEvent.RateLimited`
- [x] Base URL from `expect/actual PlatformConfig`
- [x] All methods return `Result<T>` (no raw exceptions escape)

### 1.4 Repositories

- [x] `AuthRepository` — `login()`, `register()`, `verifyEmail()`
- [x] `VehicleRepository` — `getAll()`, `getById()`, `create()`, `update()`, `delete()`, `updateState()`, `recordOdometer()`
- [x] `RentalRepository` — `getAll()`, `getById()`, `create()`, `activate()`, `complete()`, `cancel()`
- [x] `CustomerRepository` — `getAll()`, `getById()`, `create()`
- [x] `MaintenanceRepository` — `create()`, `getByVehicle()`, `start()`, `complete()`, `cancel()`
- [x] `AccountingRepository` — `createInvoice()`, `payInvoice()`, `getPayments()`, `getAccounts()`, `getPaymentMethods()`
- [x] `UserRepository` — `getUsers()`, `getUser()`, `assignRoles()`, `deleteUser()`
- [x] `TrackingRepository` — `getRoutes()`, `postLocation()`, `getVehicleState()`, `getFleetStatus()`, `getHistory()`

### 1.5 `FleetLiveClient` (WebSocket `/v1/fleet/live`)

- [x] `StateFlow<ConnectionState>` exposed (`DISCONNECTED / CONNECTING / CONNECTED / ERROR`)
- [x] `connect(token, onDelta)` and `disconnect()`
- [x] Auto-reconnect: fixed 5s delay
- [x] Ping every 30s; disconnect on Pong timeout (30s)

### 1.6 `AppDependencyDispatcher`

- [x] Decodes JWT `roles` claim
- [x] Returns sealed `UserFeatureSet`: `Backoffice` / `Driver` / `Customer` / `MultiRole`

### 1.7 `PaginatedState<T>`

- [x] Fields: `items`, `nextCursor`, `isLoadingMore`, `hasMore`
- [x] Every list ViewModel exposes `loadMore()`

### 1.8 `FieldValidator`

- [x] VIN — exactly 17 alphanumeric chars
- [x] License plate — non-blank
- [x] License expiry — future date only
- [x] Odometer — new reading > last recorded
- [x] Email — RFC 5322
- [x] Password — minimum 8 chars

### 1.9 Koin Modules

- [x] `NetworkModule` — `HttpClient`, `FleetApiClient` (commonMain)
- [x] `RepositoryModule` — all repositories (commonMain)
- [x] `TrackingModule` — `FleetLiveClient` (commonMain)
- [x] `StorageModule` — `KSafe`, `SecureStorage` actual (webMain)
- [x] `UseCaseModule` — all domain use cases (commonMain)
- [x] `ViewModelModule` — all screen ViewModels (webMain)

### 1.10 In-Memory Cache Layer

> No Room/SQLite dependency — all caching is in-process (JS single-threaded environment).

**`cache/InMemoryCache<K, V>`** (`commonMain`):

- [x] `get(key)` — returns value only if within TTL; `null` if expired or absent
- [x] `getStale(key)` — returns value regardless of TTL (SWR helpers)
- [x] `isExpired(key)` — predicate for conditional refresh logic
- [x] `put(key, value)` — stores with `Clock.System.now()` timestamp
- [x] `invalidate(key)` / `clear()` — called by write paths to maintain cache coherence

**Cache configuration per repository:**

| Repository | TTL | Rationale |
|---|---|---|
| `VehicleRepositoryImpl` | 120 s | Low mutation rate within a session |
| `RentalRepositoryImpl` | 30 s | Status changes frequently |
| `CustomerRepositoryImpl` | 120 s | Relatively stable |
| `MaintenanceRepositoryImpl` | 120 s | Schedules are stable |
| `AccountingRepositoryImpl` (invoices) | 60 s | Bulk query; updates invalidate eagerly |
| `AccountingRepositoryImpl` (payments) | 60 s | Same as invoices |
| `UserRepositoryImpl` | 120 s | Low mutation admin-only |
| `TrackingRepositoryImpl` (routes/status) | 30 s | Supplement real-time WS data |

**Cache coherence rules:**
- All **list-fetching** methods check cache before calling API
- All **mutation** methods call `.onSuccess { cache.clear() }` to invalidate stale entries
- A `forceRefresh: Boolean = false` parameter on every list-fetching method bypasses cache

**Stale-while-revalidate (SWR) strategy:**
```
Cold first load:  Loading → (network) → Success          [skeleton visible]
Warm revisit:     Loading → (cache)   → Success          [sub-frame, no skeleton]
Explicit refresh: (keep Success)      → isRefreshing=true → (network) → isRefreshing=false
```

### 1.11 Domain Layer — Use Cases (`commonMain/domain/usecase/`)

> Clean Architecture: all business logic lives in single-responsibility use cases. ViewModels **never** call repositories directly.

**Package layout:**
```
commonMain/
  domain/
    usecase/
      auth/        AuthUseCases.kt           LoginUseCase · LogoutUseCase
      vehicle/     VehicleUseCases.kt        9 use cases
      customer/    CustomerUseCases.kt       6 use cases
      rental/      RentalUseCases.kt         8 use cases
      maintenance/ MaintenanceUseCases.kt    5 use cases
      accounting/  AccountingUseCases.kt     7 use cases
      tracking/    TrackingUseCases.kt       3 use cases
      user/        UserUseCases.kt           4 use cases
      dashboard/   GetDashboardUseCase.kt    + DashboardStats · DashboardSnapshot
  di/
    UseCaseModule.kt
```

**Use case contract:**
```kotlin
class GetVehiclesUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
        state: VehicleState? = null,
        forceRefresh: Boolean = false,
    ) = repository.getVehicles(cursor, limit, state, forceRefresh)
}
```

**Implemented use case files:**
- [x] `domain/usecase/auth/AuthUseCases.kt` — `LoginUseCase`, `LogoutUseCase`
- [x] `domain/usecase/vehicle/VehicleUseCases.kt` — 9 use cases covering list, detail, CRUD, state, odometer, maintenance tab, location history
- [x] `domain/usecase/customer/CustomerUseCases.kt` — 6 use cases; `GetCustomerRentalsUseCase` applies client-side filter
- [x] `domain/usecase/rental/RentalUseCases.kt` — 8 use cases; `PayInvoiceUseCase` owns UUID idempotency-key generation
- [x] `domain/usecase/maintenance/MaintenanceUseCases.kt` — 5 use cases
- [x] `domain/usecase/accounting/AccountingUseCases.kt` — 7 use cases
- [x] `domain/usecase/tracking/TrackingUseCases.kt` — 3 use cases
- [x] `domain/usecase/user/UserUseCases.kt` — 4 use cases
- [x] `domain/usecase/dashboard/GetDashboardUseCase.kt` — `supervisorScope` parallel aggregation of 4 repositories
- [x] `di/UseCaseModule.kt` — all use cases registered as `factory { UseCase(get()) }`

---

## Phase 2 — Auth & Navigation Shell

### 2.1 Login Screen (`/login`)

- [x] Email + password form renders
- [x] Calls `POST /v1/users/login`
- [x] JWT stored in `SecureStorage` (KSafe AES-256-GCM; `sessionStorage` only — never localStorage)
- [x] Roles decoded via `AppDependencyDispatcher`
- [x] Backoffice roles (ADMIN, FLEET_MANAGER, CUSTOMER_SUPPORT, RENTAL_AGENT) → `/dashboard`
- [x] Non-backoffice roles → "Access denied" error displayed
- [x] Inline field validation via `FieldValidator` before HTTP call
- [x] Submit disabled while `UiState.Loading`

### 2.2 Router (hash-based, `AppShell.kt`)

- [x] `/login`
- [x] `/dashboard`
- [x] `/vehicles`, `/vehicles/:id`, `/vehicles/new`
- [x] `/rentals`, `/rentals/:id`, `/rentals/new`
- [x] `/customers`, `/customers/:id`, `/customers/new`
- [x] `/maintenance`, `/maintenance/:id`, `/maintenance/new`
- [x] `/accounting/invoices`, `/accounting/invoices/:id`
- [x] `/accounting/accounts`
- [x] `/accounting/payments`
- [x] `/tracking/map`
- [x] `/users`, `/users/:id` (ADMIN only)
- [x] `/architecture` (System Architecture Diagram — all roles)

### 2.3 `RouteGuard`

- [x] Token check on every navigation event
- [x] Null token → `/login`
- [x] Insufficient role → `/dashboard` + snackbar
- [x] ADMIN restriction enforced on `/users` routes

### 2.4 `AppShell`

- [x] Sidebar 240px, collapses to icon-only at < 1024px
- [x] Fleet logo, grouped nav items, user avatar + name + tooltip on hover, logout button
- [x] Active route: left border + background tint
- [x] TopBar: page title, breadcrumb (detail pages), notification slot
- [x] Nav sections: **OVERVIEW** (Dashboard), **FLEET** (Vehicles, Tracking Map), **OPERATIONS** (Rentals, Customers, Maintenance), **FINANCE** (Invoices, Payments, Accounts), **SYSTEM** (Architecture), **ADMIN** (Users — ADMIN only)

> **Icons note**: `Icons.Default.Schema` is used for the Architecture nav item. `Icons.Default.AccountTree` is NOT available in the standard Material Icons set — do not use it.

---

## Phase 3 — Dashboard Screen

**Layout:**
```
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Total       │ Active      │ Fleet Live  │ Scheduled   │
│ Vehicles    │ Rentals     │ Status      │ Maintenance │ ← KpiCard ×4
├─────────────┴─────────────┴─────────────┴─────────────┤
│                    Fleet Map Panel                      │
├─────────────────────────────┬───────────────────────────┤
│  Recent Rentals (last 5)    │  Recent Maintenance (last 5)│
└─────────────────────────────┴───────────────────────────┘
```

**Deliverables:**

- [x] `GetDashboardUseCase` — `DashboardStats` + `DashboardSnapshot`; `supervisorScope` parallel loading
- [x] `DashboardViewModel(getDashboardUseCase)` — exposes `UiState<DashboardSnapshot>`; `isRefreshing: StateFlow<Boolean>`
- [x] **KPI: Total Vehicles** — `GET /v1/vehicles` count
- [x] **KPI: Active Rentals** — `GET /v1/rentals?status=ACTIVE` count
- [x] **KPI: Fleet Live Status** — `GET /v1/tracking/fleet/status`
- [x] **KPI: Scheduled Maintenance** — count of `status=SCHEDULED`
- [x] **Recent Rentals** — `GET /v1/rentals?limit=5`
- [x] **Recent Maintenance** — maintenance list `?limit=5`
- [x] `LoadingSkeleton` shown on cold first load only
- [x] Per-panel error state (one failure does not crash others)

**Caching behavior (stale-while-revalidate):**

| Scenario | Skeleton shown? | `isRefreshing`? |
|---|---|---|
| Cold first load (no cache) | Yes — all panels | No |
| Warm revisit (within TTL) | No | No |
| Explicit refresh button | No | Yes — subtle indicator |
| Network error on refresh | No (old data retained) | No (error banner instead) |

---

## Phase 4 — Vehicles Module

### 4.1 Vehicles List (`/vehicles`)

- [x] Table columns: License Plate · Make/Model · Year · State badge · Mileage (km)
- [x] Filter bar: State filter chips (null + all VehicleState values)
- [x] "Load More" pagination
- [x] "Add Vehicle" button visible to ADMIN / FLEET_MANAGER only

### 4.2 Vehicle Detail (`/vehicles/:id`) — 5 Tabs

- [x] **Info tab** — all fields; inline edit form for ADMIN/FLEET_MANAGER (`GET/PATCH /v1/vehicles/{id}`)
- [x] **State tab** — current state badge + valid transition buttons only:

  | From | Button | Endpoint |
  |---|---|---|
  | AVAILABLE | Send to Maintenance | `PATCH state=MAINTENANCE` |
  | AVAILABLE | Retire Vehicle | `PATCH state=RETIRED` |
  | MAINTENANCE | Mark Available | `PATCH state=AVAILABLE` |
  | RENTED | (no manual transitions) | — |

- [x] **Odometer tab** — history list + "Record Reading" form (validates new > last)
- [x] **Maintenance tab** — all jobs for this vehicle with status/priority badges (`GET /v1/maintenance/vehicle/{id}`)
- [x] **Tracking History tab** — paginated location list: timestamp, lat/lon, speed, heading

### 4.3 Create Vehicle (`/vehicles/new`)

- [x] Fields: VIN (17 chars, mono font), plate, make, model, year, color
- [x] Inline validation on blur for all fields
- [x] VIN rejects non-17-char input before HTTP call

### 4.4 Use Cases (9 total in `VehicleUseCases.kt`)

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

### 4.5 Caching

| Action | Cache effect |
|---|---|
| Open Vehicles list (warm) | Returns from cache — no network request |
| Pull-to-refresh / explicit refresh | `forceRefresh = true` — bypasses cache |
| Create / Update / Delete / State change | `listCache.clear()` — next list fetch goes to network |
| Odometer record | No cache clear (individual reading) |

---

## Phase 5 — Rentals Module

### 5.1 Rentals List (`/rentals`)

- [x] Table: Rental # · Customer · Vehicle plate · Status badge · Start Date · End Date · Total (PHP)
- [x] Filter: Status filter chips (null + RESERVED/ACTIVE/COMPLETED/CANCELLED)
- [x] "New Rental" → `/rentals/new`

### 5.2 Rental Detail (`/rentals/:id`)

- [x] Summary card with all rental fields
- [x] Transition buttons (only when valid):

  | Status | Button | Action |
  |---|---|---|
  | RESERVED | Activate Rental | `POST /v1/rentals/{id}/activate` |
  | RESERVED | Cancel Rental | `POST /v1/rentals/{id}/cancel` |
  | ACTIVE | Complete Rental | `POST /v1/rentals/{id}/complete` |

- [x] "Complete Rental" requires final odometer input (validated > last recorded)
- [x] Invoice section: linked invoice status badge
- [x] "Pay Invoice" form: payment method selector from `GET /v1/accounting/payment-methods`
- [x] Pay action sends `Idempotency-Key: UUID-v4` header

### 5.3 Create Rental (`/rentals/new`)

- [x] Fields: customer selector, vehicle selector (**AVAILABLE only** — backend filters on state), start date, end date, daily rate
- [x] Dates validated: start < end; start ≥ today

### 5.4 Use Cases (8 total in `RentalUseCases.kt`)

| Use Case | Notes |
|---|---|
| `GetRentalsUseCase` | Paginated list with optional `status` filter |
| `GetRentalUseCase` | Single rental by ID |
| `CreateRentalUseCase` | Creates a new `RESERVED` rental |
| `ActivateRentalUseCase` | `RESERVED → ACTIVE` |
| `CancelRentalUseCase` | `RESERVED → CANCELLED` |
| `CompleteRentalUseCase` | `ACTIVE → COMPLETED` with final odometer |
| `GetPaymentMethodsUseCase` | Dropdown data for the Pay Invoice form |
| `PayInvoiceUseCase` | UUID idempotency key generated **inside** the use case |

### 5.5 Caching

| Action | Cache effect |
|---|---|
| Open Rentals list (warm) | Returns from `RentalRepository` listCache — no network |
| Create / Cancel / Complete rental | `RentalRepository.listCache.clear()` |
| Pay Invoice | `AccountingRepository.invoiceCache.clear()` + `paymentCache.clear()` |

---

## Phase 6 — Customers Module

### 6.1 Customers List (`/customers`)

- [x] Table: Name · Email · Phone · License # · License Expiry · Active badge
- [x] License Health `LinearProgressIndicator` column (red/amber/green by expiry proximity)
- [x] "Add Customer" → `/customers/new`
- [x] Inline deactivate toggle (`PATCH /v1/customers/{id}/deactivate`)

### 6.2 Customer Detail (`/customers/:id`)

- [x] Read-only view of all customer fields
- [x] `CustomerHealthCard` — license expiry visual indicator
- [x] Linked rentals history (`GET /v1/rentals` filtered by customer)
- [x] Linked payments history (`GET /v1/accounting/payments/customer/{id}`)

### 6.3 Create Customer (`/customers/new`)

- [x] Fields: user ID selector, first/last name, email, phone, driver license number, license expiry
- [x] License expiry validated as future date only

### 6.4 Use Cases (6 total in `CustomerUseCases.kt`)

| Use Case | Notes |
|---|---|
| `GetCustomersUseCase` | Paginated customer list |
| `GetCustomerUseCase` | Single customer by ID |
| `CreateCustomerUseCase` | New customer registration |
| `DeactivateCustomerUseCase` | Toggles `isActive` via `PATCH` |
| `GetCustomerRentalsUseCase` | Fetches all rentals; applies client-side filter: `it.customerId == customerId` |
| `GetCustomerPaymentsUseCase` | `GET /v1/accounting/payments/customer/{id}` |

---

## Phase 7 — Users Management (ADMIN Only)

### 7.1 Users List (`/users`)

- [x] Table: Name · Email · Roles · Verified · Active
- [x] Route guard: ADMIN only — non-ADMIN redirected to `/dashboard`

### 7.2 User Detail (`/users/:id`)

- [x] Profile fields (read-only)
- [x] Role assignment: multi-select chip UI (`POST /v1/users/{id}/roles`)
  - Available roles: `ADMIN`, `FLEET_MANAGER`, `CUSTOMER_SUPPORT`, `RENTAL_AGENT`, `DRIVER`, `CUSTOMER`
  - Selected roles shown as dismissible chips
  - Optimistic update; reverts on API error
- [x] Delete user: two-step `ConfirmDialog` → `DELETE /v1/users/{id}`

### 7.3 Use Cases (4 total in `UserUseCases.kt`)

- [x] `GetUsersUseCase(userRepository)`
- [x] `GetUserUseCase(userRepository)`
- [x] `AssignRolesUseCase(userRepository)`
- [x] `DeleteUserUseCase(userRepository)`

---

## Phase 8 — Accounting Module

### 8.1 Invoices (`/accounting/invoices`)

- [x] Table: Invoice # · Customer · Rental · Status badge · Amount (PHP) · Due Date
- [x] "Create Invoice" form: linked rental selector + due date

### 8.2 Invoice Detail (`/accounting/invoices/:id`)

- [x] Invoice summary card
- [x] "Pay" action: modal with payment method selector (`GET /v1/accounting/payment-methods`)
- [x] Fresh `Idempotency-Key: UUID-v4` generated per pay attempt before `POST /v1/accounting/invoices/{id}/pay`
- [x] OVERDUE invoices: due date field highlighted with `#EF4444` tint

### 8.3 Chart of Accounts (`/accounting/accounts`)

- [x] Grouped by `AccountType`: ASSET · LIABILITY · EQUITY · REVENUE · EXPENSE
- [x] Rows: account code (mono font) · name · type · balance
- [x] Negative balances rendered in `#EF4444`

### 8.4 Payments (`/accounting/payments`)

- [x] Table: Invoice # · Customer · Amount (PHP) · Payment Method · Date
- [x] Filter: date range, payment method

### 8.5 Use Cases (7 in `AccountingUseCases.kt` + `PayInvoiceUseCase` in `RentalUseCases.kt`)

- [x] `GetInvoicesUseCase`, `GetInvoiceUseCase`
- [x] `CreateInvoiceUseCase`
- [x] `GetPaymentsUseCase`, `GetPaymentsByCustomerUseCase`
- [x] `GetAccountsUseCase`
- [x] `GetPaymentMethodsUseCase`
- [x] `PayInvoiceUseCase` — in `RentalUseCases.kt`; reused in accounting context

### 8.6 Caching

| Action | Cache effect |
|---|---|
| Open Invoices list (warm) | Returns from `invoiceCache` — no network |
| Open Payments list (warm) | Returns from `paymentCache` — no network |
| Create Invoice | `invoiceCache.clear()` |
| Pay Invoice | `invoiceCache.clear()` + `paymentCache.clear()` |
| Chart of Accounts | Session-lifetime `StateFlow` in `AccountsViewModel`; explicit refresh only |
| Payment Methods | Not cached (lightweight dropdown, always fresh) |

---

## Phase 9 — Maintenance Module

### 9.1 Maintenance List (`/maintenance`)

- [x] Table: Job # · Vehicle · Type badge · Priority badge · Status badge · Scheduled Date · Est. Cost
- [x] Filter: Priority, Type, Status, Vehicle
- [x] "Schedule Job" → `/maintenance/new`

### 9.2 Maintenance Detail (`/maintenance/:id`)

- [x] Job info card
- [x] Transition buttons (only when valid):

  | Status | Button | Action |
  |---|---|---|
  | SCHEDULED | Start Job | `POST /v1/maintenance/{id}/start` |
  | SCHEDULED | Cancel Job | `POST /v1/maintenance/{id}/cancel` |
  | IN_PROGRESS | Complete Job | `POST /v1/maintenance/{id}/complete` |

- [x] "Complete Job" form: `laborCost` + `partsCost` inputs (both must be positive numbers)
- [x] Cost summary card: labor + parts + total

### 9.3 Schedule Maintenance (`/maintenance/new`)

- [x] Fields: vehicle selector (with identity info + usage history), job type, priority, scheduled date, estimated cost, description

### 9.4 Use Cases (5 in `MaintenanceUseCases.kt`)

- [x] `GetMaintenanceJobsUseCase`, `GetMaintenanceJobUseCase`
- [x] `ScheduleMaintenanceUseCase` — `POST /v1/maintenance`
- [x] `CompleteMaintenanceUseCase` — with labor + parts cost
- [x] `CancelMaintenanceUseCase`
- [x] `GetVehicleMaintenanceUseCase` — in `VehicleUseCases.kt`; reused for Vehicle Detail → Maintenance tab

---

## Phase 10 — Fleet Tracking Map (`/tracking/map`)

> **Implementation**: Compose Multiplatform canvas map (OSM tiles via Coil 3 + Web Mercator projection). Same WebSocket state flow and animation system as planned.

### 10.1 Core Utilities

- [x] `SvgUtils.wktToPoints(lineString)` — converts PostGIS WKT LineString → `List<Pair<Double,Double>>`
- [x] `MapProjection.toCanvasXY(lat, lon, mapState, w, h)` — Web Mercator (EPSG:3857) → canvas pixel coordinate
- [x] `MapViewState` — immutable viewport state; `zoomedIn()`, `zoomedOut()`, `panned(dx, dy)` return copies; `MIN_ZOOM = 3`, `MAX_ZOOM = 18`
- [x] `FleetState` — `MutableStateFlow<Map<VehicleId, VehicleRouteState>>` wired to `FleetLiveClient`

### 10.2 Map Canvas (`FleetMapCanvas`)

- [x] **Fixed-lens viewport**: outer `Box(clipToBounds)` + inner `Box(requiredSize(1920.dp, 1080.dp))` fixed projection canvas
- [x] **OSM tile layer** (`OsmTileLayer`): Coil 3 `AsyncImage` per tile; horizontal X wrapping; no API key
- [x] **Route polylines**: `Canvas` layer draws WKT routes as `Path` strokes
- [x] **Car icon markers**: `painterResource(Res.drawable.car_top)`; `absoluteOffset`-positioned; `Modifier.rotate(headingDeg)`; 500ms `animateFloatAsState` tween
- [x] **Selected vehicle**: 44dp icon with translucent ring; unselected 32dp
- [x] **Drag-to-pan**: `detectDragGestures`; `MapViewState.panned(dx, dy)` with inverse Web Mercator math; hand cursor
- [x] **Zoom controls**: `+`/`−` `IconButton`s; disabled at `MIN_ZOOM`/`MAX_ZOOM`
- [x] Right sidebar: vehicle list sorted by status; click → highlight + select marker
- [x] Selection info panel: plate, speed, heading, route progress %, last update timestamp
- [x] **OSM attribution**: `© OpenStreetMap contributors` pinned bottom-right
- [x] Status bar: `ConnectionState` indicator — Connected (green) / Reconnecting (amber pulse) / Offline (red)
- [x] **GeoJSON import**: `ImportRouteDialog` + `vm.importRoute()` — paste GeoJSON from geojson.io, saves to backend, reloads map

### 10.3 WebSocket Connection Rules

- [x] `CONNECTING` state on connect; `Authorization: Bearer {token}` in header
- [x] Heartbeat: Ping every 30s; Pong timeout disconnect (30s)
- [x] Auto-reconnect: fixed 5s delay on any disconnect/error
- [x] `disconnect()` called on route unmount

### 10.4 Use Cases (3 in `TrackingUseCases.kt`)

- [x] `GetFleetStatusUseCase(trackingRepository)` — with `forceRefresh`
- [x] `GetActiveRoutesUseCase(trackingRepository)` — with `forceRefresh`
- [x] `GetVehicleStateUseCase(trackingRepository)`
- [x] `fleetLiveClient` injected **directly** into `FleetTrackingViewModel` — infrastructure, not a use case

### 10.5 Performance Targets

| Metric | Target | Critical threshold |
|---|---|---|
| FPS (50 vehicles) | ≥ 55 fps | < 30 fps |
| Render time per frame | < 16ms | > 33ms |
| WS message → render | < 100ms | > 500ms |
| Time to first render | < 2s | > 5s |
| Memory usage | < 100MB | > 500MB |

### 10.6 Caching

| Data source | Cache | Rationale |
|---|---|---|
| `GET /v1/tracking/routes` | 30 s TTL in `routesCache` | Geometry rarely changes mid-session |
| `GET /v1/tracking/fleet/status` | 30 s TTL in `statusCache` | Supplements WS data |
| `GET /v1/tracking/{id}/history` | **Not cached** | Explicit user-initiated queries |
| WebSocket `VehicleStateDelta` | `FleetState: MutableStateFlow<...>` | In-memory live state; updated every WS frame |

---

## Design System (Phase 0)

**All components are implemented in `commonMain/ui/` and `webMain/`:**

- [x] `FleetTheme.kt` — MaterialTheme override with color tokens, typography scale, shape tokens
- [x] `StatusBadge.kt` — reusable pill for all 14 status/priority variants
- [x] `LoadingSkeleton.kt` — shimmer placeholder for every async panel
- [x] `ConfirmDialog.kt` — modal with title, message, confirm/cancel actions
- [x] `KpiCard.kt` — bento-style summary card: icon + value + label
- [x] `PaginatedTable.kt` — generic sortable/filterable table with load-more

**Design Tokens:**
- Primary: `#1E40AF` · Accent: `#F59E0B` · Surface: `#F8FAFC`
- Typography: Inter (Google Fonts) · JetBrains Mono for code/IDs
- Hover transitions: 200ms ease on all interactive elements
- Touch targets: all ≥ 44px tall
- Focus ring: 2px solid `#3B82F6` on keyboard focus

---

## Architecture Visualization (System Feature)

> **Route**: `/architecture` · **Nav icon**: `Icons.Default.Schema`
> **Location**: `composeApp/src/webMain/kotlin/.../features/architecture/ArchitectureScreen.kt`
> **Component**: `MermaidViewer.kt` in `webMain/components/common/`

- [x] `ArchitectureScreen` — displays the project architecture as a Mermaid.js diagram
- [x] `MermaidViewer` — wasmJs-compatible component; renders Mermaid diagrams via `window.renderMermaid()` JS interop
- [x] Mermaid.js loaded from CDN in `index.html`
- [x] K2D KSP plugin configured on `kspCommonMainMetadata` only (not `kspWasmJs` or `kspAndroid`)

**wasmJs interop rules (MermaidViewer.kt):**
- Use `style.setProperty("key", "value")` — not direct property access (e.g., `style.overflow`)
- `js()` calls must be in top-level block-body functions with explicit `: Unit` return type
- Do NOT use `asDynamic()` — deprecated in wasmJs; use `js()` top-level interop or `@JsFun` external declaration

---

## Open Items / Known Issues

| Issue | Status |
|---|---|
| Kotlin compiler version mismatch (metadata 2.3.0 vs IDE compiler 2.1.0) | Lint warnings only — does not affect build; resolve by upgrading IDE Kotlin plugin |
| `androidTarget` deprecation warning (AGP 9.0.0 future break) | Non-blocking warning; requires KMP project structure migration when AGP 9.0.0 releases |
| `FrontendMetrics` (render/WS/FPS instrumentation) | Not implemented — deferred to Phase 12 |
| K2D auto-generated architecture diagram | Currently hardcoded Mermaid chart; auto-generation from KSP output is future work |
| E2E tests (Playwright) | Deferred to Phase 11 |
