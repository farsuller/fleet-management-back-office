# Phase 1 — Shared Core Infrastructure

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 0
> [← Back to master plan](../web-backoffice-implementation-plan.md)

**Goal**: all platform-agnostic business-layer code; web, Android, and iOS consume this without changes.

---

## 1.0 Gradle Setup

- [ ] Gradle sync succeeds with no missing-dependency errors
- [ ] `kotlinSerialization` plugin applied

| Library | Version |
|---|---|
| Ktor Client | `3.1.2` |
| Koin | `4.0.2` |
| Coil | `3.1.0` |
| kotlinx.serialization | `1.8.0` |
| kotlinx.coroutines | `1.10.1` |
| kotlinx.datetime | `0.6.2` |

## 1.1 API DTOs (`commonMain/api/dto/`)

- [x] `auth/` — `LoginRequest`, `LoginResponse`, `UserDto`
- [x] `vehicle/` — `VehicleDto`, `VehicleStateRequest`
- [x] `rental/` — `RentalDto`, `CreateRentalRequest`
- [x] `customer/` — `CustomerDto`
- [x] `maintenance/` — `MaintenanceJobDto`
- [x] `accounting/` — `InvoiceDto`, `PaymentDto`, `AccountDto`
- [x] `tracking/` — `VehicleStateDto`, `VehicleStateDelta`, `VehicleRouteState`, `RouteDto`
- [x] All monetary fields as `Long`; timestamps as `Long`
- [x] All enums use `@SerialName` + `UNKNOWN` fallback
- [x] **All response DTO fields are nullable (`? = null`)** — prevents deserialization crashes when the backend omits optional fields; screens handle null with `?.` safe-calls throughout

## 1.2 `ApiResponse<T>` + `ApiError`

- [ ] `ApiResponse<T>` envelope with `success`, `data`, `error`, `requestId`
- [ ] `ApiError` with `code`, `message`, `fieldErrors: List<FieldError>`

## 1.3 `FleetApiClient`

- [ ] Ktor `HttpClient` configured with `ContentNegotiation` + JSON
- [ ] `Authorization: Bearer` injected via `HttpClientPlugin` from `SecureStorage`
- [ ] On `401` → clear token + emit `AuthEvent.Unauthorized` on `SharedFlow`
- [ ] On `429` → emit `AuthEvent.RateLimited`
- [ ] Base URL from `expect/actual PlatformConfig`
- [ ] All methods return `Result<T>` (no raw exceptions escape)

## 1.4 Repositories

- [ ] `AuthRepository` — `login()`, `register()`, `verifyEmail()`
- [ ] `VehicleRepository` — `getAll()`, `getById()`, `create()`, `update()`, `delete()`, `updateState()`, `recordOdometer()`
- [ ] `RentalRepository` — `getAll()`, `getById()`, `create()`, `activate()`, `complete()`, `cancel()`
- [ ] `CustomerRepository` — `getAll()`, `getById()`, `create()`
- [ ] `MaintenanceRepository` — `create()`, `getByVehicle()`, `start()`, `complete()`, `cancel()`
- [ ] `AccountingRepository` — `createInvoice()`, `payInvoice()`, `getPayments()`, `getAccounts()`, `getPaymentMethods()`
- [ ] `TrackingRepository` — `getRoutes()`, `postLocation()`, `getVehicleState()`, `getFleetStatus()`, `getHistory()`

## 1.5 `FleetLiveClient` (WebSocket `/v1/fleet/live`)

- [ ] `StateFlow<ConnectionState>` exposed (`DISCONNECTED / CONNECTING / CONNECTED / ERROR`)
- [ ] `connect(token, onDelta)` and `disconnect()`
- [ ] Auto-reconnect: fixed 5s delay
- [ ] Ping every 30s; disconnect on Pong timeout (30s)

## 1.6 `AppDependencyDispatcher`

- [ ] Decodes JWT `roles` claim
- [ ] Returns sealed `UserFeatureSet`: `Backoffice` / `Driver` / `Customer` / `MultiRole`

## 1.7 `PaginatedState<T>`

- [ ] Fields: `items`, `nextCursor`, `isLoadingMore`, `hasMore`
- [ ] Every list ViewModel exposes `loadMore()`

## 1.8 `FieldValidator`

- [ ] VIN — exactly 17 alphanumeric chars
- [ ] License plate — non-blank
- [ ] License expiry — future date only
- [ ] Odometer — new reading > last recorded
- [ ] Email — RFC 5322
- [ ] Password — minimum 8 chars

## 1.9 Koin Modules

- [x] `NetworkModule` — `HttpClient`, `FleetApiClient` (commonMain)
- [x] `RepositoryModule` — all 7 repositories (commonMain)
- [x] `TrackingModule` — `FleetLiveClient` (commonMain)
- [x] `StorageModule` — `KSafe`, `SecureStorage` actual (webMain)
- [x] `UseCaseModule` — all domain use cases (commonMain) ← **added with Clean Architecture**
- [x] `ViewModelModule` — all screen ViewModels (webMain)

> **Module load order in `main.kt`**: `storageModule → networkModule → trackingModule → repositoryModule → useCaseModule → viewModelModule`
> Each layer only depends on the layer below it; Koin resolves all bindings at start-up.

## 1.10 In-Memory Cache Layer

> **Why not Room?** Room KMP has no Kotlin/JS (browser) driver — SQLite cannot run in a browser sandbox. The correct caching layers for a `webMain` target are in-memory (session) and IndexedDB (cross-refresh).

### `cache/InMemoryCache<K, V>` (`commonMain`)

- [ ] `get(key)` — returns value only if within TTL; `null` if expired or absent
- [ ] `getStale(key)` — returns value regardless of TTL (reserved for composite SWR helpers)
- [ ] `isExpired(key)` — predicate for conditional refresh logic
- [ ] `put(key, value)` — stores with `Clock.System.now()` timestamp
- [ ] `invalidate(key)` / `clear()` — called by write paths to maintain cache coherence
- [ ] Backed by `LinkedHashMap`; no cross-coroutine synchronisation needed (single JS thread)

### Cache configuration per repository

| Repository | TTL | Rationale |
|---|---|---|
| `VehicleRepositoryImpl` | 120 s | Low mutation rate within a session |
| `RentalRepositoryImpl` | 30 s | Status changes frequently (activate / cancel / complete) |
| `MaintenanceRepositoryImpl` | 120 s | Schedules are relatively stable |
| `AccountingRepositoryImpl` (invoices) | 60 s | Bulk query is expensive; updates invalidate eagerly |
| `AccountingRepositoryImpl` (payments) | 60 s | Same as invoices |

### Cache coherence rules

- All **list-fetching** methods (`getVehicles`, `getRentals`, `getJobs`, `getInvoices`, `getPayments`) check cache before calling the API.
- All **mutation** methods (`create*`, `update*`, `cancel*`, `complete*`, `delete*`) call `.onSuccess { cache.clear() }` to invalidate stale entries.
- A `forceRefresh: Boolean = false` parameter on every list-fetching method allows the ViewModel to bypass the cache without clearing it — the fresh result then replaces the old cache entry.

### Stale-while-revalidate (SWR) in `DashboardViewModel`

Because all 4 repository calls resolve from in-memory cache almost instantaneously on revisit, the dashboard `UiState.Loading` phase is invisible to the user (completes within a single frame). The explicit `refresh()` action uses `forceRefresh = true` and keeps current data visible via the `isRefreshing: StateFlow<Boolean>` flag, allowing the screen to show a subtle progress indicator without triggering a skeleton re-paint.

```
Cold first load:  Loading → (network) → Success          [skeleton visible]
Warm revisit:     Loading → (cache)   → Success          [sub-frame, no skeleton]
Explicit refresh: (keep Success)      → isRefreshing=true → (network) → isRefreshing=false
```

## Verification

- [ ] All DTO classes compile error-free with `kotlinx.serialization`
- [ ] `FleetApiClient` round-trip tested against live backend (login + token store)
- [ ] 401 interception clears session and emits `Unauthorized` event
- [ ] `FieldValidator` unit tests pass for all 6 rules and edge cases
- [ ] `startKoin { modules(...) }` loads at app startup with no missing-binding errors
- [ ] Second navigation to Dashboard shows no visible skeleton (cache hit verified in DevTools network tab showing 0 requests)
- [ ] Mutation (cancel rental) followed by Dashboard revisit shows updated count (cache invalidated)

---

## 1.11 Domain Layer — Use Cases (`commonMain/domain/usecase/`)

> **Clean Architecture.** All business logic previously in ViewModels is now in single-responsibility use cases. ViewModels only orchestrate UI state and **never** call repositories directly.

### Package layout

```
commonMain/
  domain/
    usecase/
      auth/        AuthUseCases.kt           LoginUseCase · LogoutUseCase
      vehicle/     VehicleUseCases.kt         9 use cases
      customer/    CustomerUseCases.kt        6 use cases
      rental/      RentalUseCases.kt          8 use cases
      dashboard/   GetDashboardUseCase.kt     + DashboardStats · DashboardSnapshot
  di/
    UseCaseModule.kt
```

### Use case contract

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

### Dependency rule

```
Presentation  →  Domain (use cases)  →  Data (repositories)  →  Infrastructure (API client)
```

No ViewModel, no Compose, and no Ktor imports are allowed inside the domain layer.

### Adding a use case — checklist (required for every future feature phase)

1. Create `domain/usecase/<feature>/<Feature>UseCases.kt`
2. Register each as `factory { UseCase(get()) }` in `UseCaseModule.kt`
3. Inject use cases into the ViewModel constructor — **never inject a repository into a ViewModel**
4. Register the ViewModel in `ViewModelModule.kt` with named use-case parameters

### Implemented use case files

- [x] `domain/usecase/auth/AuthUseCases.kt` — `LoginUseCase`, `LogoutUseCase`
- [x] `domain/usecase/vehicle/VehicleUseCases.kt` — 9 use cases covering list, detail, CRUD, state, odometer, maintenance tab, and location history
- [x] `domain/usecase/customer/CustomerUseCases.kt` — 6 use cases; `GetCustomerRentalsUseCase` applies client-side filter
- [x] `domain/usecase/rental/RentalUseCases.kt` — 8 use cases; `PayInvoiceUseCase` owns UUID idempotency-key generation
- [x] `domain/usecase/dashboard/GetDashboardUseCase.kt` — owns `DashboardStats` + `DashboardSnapshot` models; `supervisorScope` parallel aggregation of 4 repositories
- [x] `di/UseCaseModule.kt` — all use cases registered as `factory { UseCase(get()) }`
