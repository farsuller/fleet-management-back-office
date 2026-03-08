# Phase 11 — Testing & Quality Assurance

> **Status**: `NOT STARTED`
> **Prerequisite**: Phases 4–10
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## 11.1 Unit Tests (`commonTest`)

- [ ] `DeltaDecoder.merge(null, delta)` — creates full state from scratch
- [ ] `DeltaDecoder.merge(existing, partial)` — retains fields absent from delta
- [ ] `SvgUtils.polylineToPath("")` → returns `""`
- [ ] `SvgUtils.polylineToPath(valid)` → starts with `M`
- [ ] `SvgUtils.getPointAtProgress(…, 0.0)` → first coordinate
- [ ] `SvgUtils.getPointAtProgress(…, 1.0)` → last coordinate
- [ ] `FieldValidator` — all 6 rules pass valid input and reject invalid with correct messages
- [ ] `ApiResponse<T>` — `success=false` maps to typed error; `fieldErrors` array populated

### 11.1.2 `InMemoryCache` Unit Tests

- [ ] `get(key)` returns `null` when cache is empty
- [ ] `put` then `get` within TTL → returns value
- [ ] `put` then `get` after TTL artificially advanced → returns `null` (use `kotlinx.datetime.Clock.System` teslt clock or advance `cachedAt` via reflection)
- [ ] `getStale` returns value even after TTL expiry
- [ ] `isExpired` returns `true` for missing key and expired entry; `false` for fresh entry
- [ ] `invalidate(key)` removes entry; other keys unaffected
- [ ] `clear()` removes all entries

### 11.1.3 Repository Cache Integration Tests

For each of the 6 cached repositories (`Vehicle`, `Rental`, `Customer`, `Maintenance`, `AccountingInvoices`, `AccountingPayments`, `Tracking` routes/status):

- [ ] **Cache hit** — `getXxx()` called twice; `MockEngine` asserts exactly **1** network request
- [ ] **Cache miss after TTL** — second call after TTL → `MockEngine` asserts **2** network requests
- [ ] **`forceRefresh = true`** — bypasses cache; `MockEngine` asserts **2** requests even if TTL not expired
- [ ] **Mutation invalidates list** — create/update/cancel → `getXxx()` → `MockEngine` asserts network request on the post-mutation list call

All tests use `runTest` + injected `TestDispatcher` (no `GlobalScope`).

### 11.1.4 Use Case Unit Tests

For each use case in `domain/usecase/`:

- [ ] **Single-repo use cases** (e.g. `GetVehiclesUseCase`): create a `FakeVehicleRepository`, call `invoke(...)`, assert result equals what the fake returned — no network involved
- [ ] **`GetDashboardUseCase`** (multi-repo): mock all 4 repositories; assert `DashboardStats` counts are calculated correctly; simulate one repo returning `Result.failure` → verify overall result is `Result.failure` (supervisorScope still catches sibling jobs)
- [ ] **`GetCustomerRentalsUseCase`**: populate fake repository with rentals from multiple customers; assert client-side filter `it.customerId == customerId` returns only the expected records
- [ ] **`PayInvoiceUseCase`**: call twice; assert that the idempotency key passed to the repository is a valid UUID **and** that the two keys are distinct (new UUID per invocation)
- [ ] All use case tests live in `commonTest/domain/usecase/` using `runTest` + `TestDispatcher`; no `GlobalScope`

## 11.1.1 Koin DI Tests

- [ ] `KoinTestRule` verifies all production modules load without missing bindings (`checkModules {}`)
- [ ] ViewModel unit tests use `fakeRepositoryModule` override pattern
- [ ] `stopKoin()` called in every `@AfterTest`

## 11.2 Integration Tests (`webTest`)

- [ ] `FleetLiveClient`: connect → receive delta → `FleetState` updated → disconnect → reconnect after 5s
- [ ] Mocked via `MockEngine` from Ktor test utilities

## 11.3 Performance Tests

- [ ] Fleet map renders 50 vehicle updates within 16ms per frame (`FrontendMetrics.lastRenderTime < 16.0`)

## 11.4 E2E Tests (Playwright)

- [ ] Login → Dashboard loads KPI cards with real data
- [ ] Create Vehicle → appears in vehicle list
- [ ] Vehicle state transition → state badge updates correctly
- [ ] Pay Invoice → `Idempotency-Key` present in request headers
- [ ] Fleet Tracking Map → WebSocket connects → vehicle markers appear

## Coverage Target

- [ ] Unit + integration coverage **≥ 75%** for all `commonMain` and `webMain` modules
- [ ] All E2E flows pass before Phase 12 begins
