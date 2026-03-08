# Fleet Management — Backoffice Web Portal: Implementation Plan

> **Target**: Kotlin/JS (IR) + Compose for Web  
> **Backend**: Ktor + PostgreSQL + Redis (live — do not modify)  
> **Skills applied**: kotlin-specialist · kotlin-coroutines-expert · ui-ux-pro-max

---

## Design System

Derived from **ui-ux-pro-max** for a fleet operations SaaS backoffice dashboard.

### Product Profile
| Dimension     | Decision                                                                 |
|---------------|--------------------------------------------------------------------------|
| Product type  | Operational SaaS / B2B Fleet Analytics Dashboard                         |
| Style         | **Bento Grid** (dashboard cards) + **Data-Dense Dashboard** (tables) + **Real-Time Monitoring** (fleet map) |
| Color palette | Analytics Dashboard + Fleet-status semantic layer                        |
| Typography    | **Inter** throughout — Minimal Swiss single-family; weight-based hierarchy |
| Stack         | Jetpack Compose (Compose for Web / Kotlin/JS IR)                         |

### Color Tokens

```
Primary      #1E40AF   — interactive elements, links, primary actions
Secondary    #3B82F6   — hover states, secondary actions
Accent       #F59E0B   — highlights, badges, warnings
Surface      #F8FAFC   — page background (light mode)
Surface-2    #F1F5F9   — card/panel background
Border       #E2E8F0   — dividers, table borders
Text-1       #1E293B   — headings, primary text
Text-2       #64748B   — labels, secondary text

// Status semantic colors (Vehicle, Rental, Maintenance)
available    #22C55E   — AVAILABLE
rented       #3B82F6   — RENTED
maintenance  #F97316   — MAINTENANCE
retired      #94A3B8   — RETIRED
reserved     #EAB308   — RESERVED / SCHEDULED
active       #22C55E   — ACTIVE
completed    #94A3B8   — COMPLETED
cancelled    #EF4444   — CANCELLED
low          #94A3B8   — Priority LOW
normal       #3B82F6   — Priority NORMAL
high         #F97316   — Priority HIGH
urgent       #EF4444   — Priority URGENT

// Fleet Map overlay (dark schematic)
map-bg       #0F172A   — SVG canvas background
map-route    #334155   — route path stroke
map-connect  #22C55E   — WebSocket connected
map-offline  #EF4444   — WebSocket disconnected
```

### Typography Scale

```
heading-xl   Inter 700  32px / 1.2   — page titles
heading-l    Inter 600  24px / 1.3   — section titles
heading-m    Inter 600  18px / 1.4   — card headers, tabs
body         Inter 400  14px / 1.5   — body text, table cells
caption      Inter 400  12px / 1.4   — labels, timestamps
badge        Inter 500  11px / 1.0   — status pill text
mono         JetBrains Mono 400 13px — VIN fields, coordinates, IDs
```

### Spacing & Shape
- Base unit: `8px`
- Card radius: `12px`
- Table row height: `44px` (touch-target compliant)
- Sidebar width: `240px`
- Header height: `56px`
- Card padding: `16px / 24px`
- Bento grid gap: `16px`
- Status badge padding: `2px 8px`, radius `9999px`

### Chart Guidance (from ui-ux-pro-max)
| Screen                | Chart type                           | Library           |
|-----------------------|--------------------------------------|-------------------|
| Dashboard KPIs        | Sparkline (trend, line)              | Custom SVG / Canvas |
| Fleet map vehicle pos | Animated SVG polygon + path          | Custom SVG        |
| Maintenance cost      | Bar chart (labor + parts stacked)    | Custom SVG        |
| Accounting revenue    | Line chart + area fill               | Custom SVG / Canvas |

### Compose for Web UX Rules
- Hover states: `200ms` ease transition on cards and table rows
- Loading skeletons on every async panel (no spinners alone)
- Status badges always carry both icon and text (color is not the only indicator — WCAG)
- Confirmation dialogs for all destructive actions (delete, cancel, retire)
- Snackbar/toast for non-blocking feedback; error inline for form validation
- Keyboard navigation: `Tab` through tables, `Enter` to open detail, `Esc` to close dialogs
- Empty states: illustration + actionable CTA text, never blank panels

---

## Architecture Conventions

> Applied from **kotlin-specialist** + **kotlin-coroutines-expert** skills.

### UiState Pattern

```kotlin
// commonMain — used by every ViewModel and Composable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : UiState<Nothing>
}
```

### ViewModel Pattern

Every screen has one `ScreenModel` / `ViewModel` with a private `MutableStateFlow` and exposed `StateFlow`.  
Parallel dashboard aggregation **must** use `supervisorScope` so one failing API call does not cancel siblings.

```kotlin
// Pattern enforced by kotlin-specialist skill
class VehicleListViewModel(private val repo: VehicleRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<VehicleDto>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<VehicleDto>>> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = withContext(Dispatchers.Default) { repo.getVehicles() }
                _uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### Coroutine Rules (kotlin-coroutines-expert)

| Rule                                   | Enforcement                                                  |
|----------------------------------------|--------------------------------------------------------------|
| No `GlobalScope`                       | All scopes are `viewModelScope` or structured `coroutineScope` |
| Parallel aggregation                   | `supervisorScope { async { } + async { } }` pattern         |
| Reactive search/filter                 | `.debounce(300).flatMapLatest { repo.search(it) }`          |
| State retained / events fire-once      | `StateFlow` for state; `SharedFlow(replay=0)` for events    |
| WebSocket reconnect                    | Fixed 5s delay on web; no `runBlocking` anywhere            |
| Never catch `CancellationException`    | Unless immediately rethrowing                               |
| Testing                                | `TestScope` + `runTest` + injected `TestDispatcher`         |

### expect/actual Boundaries
Only platform-specific code lives in `*Main` source sets.  
`SecureStorage` is the primary `expect/actual` split for this module:

```kotlin
// commonMain
expect class SecureStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
    fun saveUserId(id: String)
    fun getUserId(): String?
}

// webMain — sessionStorage only (no persistent JWT on web)
actual class SecureStorage : SecureStorage {
    actual fun saveToken(token: String) = window.sessionStorage.setItem("jwt", token)
    actual fun getToken(): String? = window.sessionStorage.getItem("jwt")
    actual fun clearToken() = window.sessionStorage.removeItem("jwt")
    actual fun saveUserId(id: String) = window.sessionStorage.setItem("uid", id)
    actual fun getUserId(): String? = window.sessionStorage.getItem("uid")
}
```

### Dependency Injection (Koin)

All dependencies are wired via **Koin 4.x** (`koin-core` in `commonMain`, `koin-compose-viewmodel` in `webMain`).  
Manual construction of repositories, API clients, or ViewModels outside of Koin modules is not permitted anywhere in production code.

#### Koin module breakdown

| Module file | Source set | Scope | Contents |
|---|---|---|---|
| `NetworkModule.kt` | `commonMain` | `single` | `HttpClient`, `FleetApiClient` |
| `RepositoryModule.kt` | `commonMain` | `single` | All 7 repositories |
| `TrackingModule.kt` | `commonMain` | `single` | `FleetLiveClient`, `FleetState` |
| `StorageModule.kt` | `webMain` | `single` | `SecureStorage` (actual) |
| `ViewModelModule.kt` | `webMain` | `factory` / `viewModel` | All screen ViewModels |

#### App initialization (`webMain/App.kt`)

```kotlin
fun main() {
    startKoin {
        modules(networkModule, repositoryModule, trackingModule, storageModule, viewModelModule)
    }
    // launch Compose root
}
```

#### ViewModel injection in Composables

```kotlin
// Use koin-compose-viewmodel — never instantiate ViewModels directly
@Composable
fun VehicleListScreen() {
    val viewModel: VehicleListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    // ...
}
```

#### Testing with Koin

Use `koin-test` + `KoinTestRule` (JUnit 4) or manual `startKoin` / `stopKoin` in `@BeforeTest` / `@AfterTest`.  
Override production modules with fake/mock implementations:

```kotlin
val testModule = module {
    single<VehicleRepository> { FakeVehicleRepository() }
    viewModel { VehicleListViewModel(get()) }
}
```

---

## Module Structure

```
composeApp/
├── src/
│   ├── commonMain/kotlin/org/solodev/fleet/mngt/
│   │   ├── api/
│   │   │   ├── FleetApiClient.kt          ← Ktor HttpClient, JWT plugin, ApiResponse<T>
│   │   │   ├── ApiResponse.kt             ← envelope deserialization
│   │   │   └── dto/
│   │   │       ├── auth/                  ← LoginRequest, LoginResponse, UserDto
│   │   │       ├── vehicle/               ← VehicleDto, VehicleStateRequest
│   │   │       ├── rental/                ← RentalDto, CreateRentalRequest
│   │   │       ├── customer/              ← CustomerDto
│   │   │       ├── maintenance/           ← MaintenanceJobDto
│   │   │       ├── accounting/            ← InvoiceDto, PaymentDto, AccountDto
│   │   │       └── tracking/              ← VehicleStateDto, VehicleStateDelta, VehicleRouteState, RouteDto
│   │   ├── repository/
│   │   │   ├── AuthRepository.kt
│   │   │   ├── VehicleRepository.kt
│   │   │   ├── RentalRepository.kt
│   │   │   ├── CustomerRepository.kt
│   │   │   ├── MaintenanceRepository.kt
│   │   │   ├── AccountingRepository.kt
│   │   │   └── TrackingRepository.kt
│   │   ├── auth/
│   │   │   ├── SecureStorage.kt           ← expect class
│   │   │   ├── AppDependencyDispatcher.kt ← JWT role decoder → feature graph
│   │   │   └── AuthState.kt               ← StateFlow<AuthStatus>
│   │   ├── tracking/
│   │   │   ├── FleetLiveClient.kt         ← WS /v1/fleet/live, reconnect, ping/pong
│   │   │   ├── VehicleStateDelta.kt       ← partial update model
│   │   │   └── VehicleRouteState.kt       ← full vehicle state on a route
│   │   ├── ui/
│   │   │   ├── UiState.kt
│   │   │   └── PaginatedState.kt
│   │   ├── validation/
│   │   │   └── FieldValidator.kt          ← VIN, plate, expiry, odometer, email, password
│   │   └── di/
│   │       ├── NetworkModule.kt           ← HttpClient, FleetApiClient Koin module
│   │       ├── RepositoryModule.kt        ← all 7 repositories Koin module
│   │       └── TrackingModule.kt          ← FleetLiveClient, FleetState Koin module
│   ├── webMain/kotlin/org/solodev/fleet/mngt/
│   │   ├── App.kt                         ← Compose entry, router, theme
│   │   ├── auth/
│   │   │   └── SecureStorage.kt           ← actual (sessionStorage)
│   │   ├── navigation/
│   │   │   ├── Router.kt                  ← hash/history routing
│   │   │   └── RouteGuard.kt              ← role-based route protection
│   │   ├── theme/
│   │   │   └── FleetTheme.kt              ← design tokens, MaterialTheme overrides
│   │   ├── components/
│   │   │   ├── layout/
│   │   │   │   ├── AppShell.kt            ← sidebar + topbar scaffold
│   │   │   │   ├── Sidebar.kt
│   │   │   │   └── TopBar.kt
│   │   │   ├── common/
│   │   │   │   ├── StatusBadge.kt         ← vehicle/rental/maintenance/priority badges
│   │   │   │   ├── PaginatedTable.kt      ← generic sortable/filterable table
│   │   │   │   ├── KpiCard.kt             ← bento-style summary card
│   │   │   │   ├── ConfirmDialog.kt
│   │   │   │   └── LoadingSkeleton.kt
│   │   │   └── fleet/
│   │   │       ├── FleetMap.kt            ← SVG canvas root
│   │   │       ├── RouteLayer.kt          ← SVG path per route
│   │   │       ├── VehicleIcon.kt         ← animated SVG polygon + 500ms tween
│   │   │       └── WebSocketStatus.kt     ← connection indicator
│   │   ├── screens/
│   │   │   ├── login/
│   │   │   ├── dashboard/
│   │   │   ├── vehicles/
│   │   │   ├── rentals/
│   │   │   ├── customers/
│   │   │   ├── maintenance/
│   │   │   ├── accounting/
│   │   │   ├── tracking/
│   │   │   └── users/
│   │   ├── state/
│   │   │   ├── FleetState.kt              ← MutableStateFlow<Map<VehicleId, VehicleRouteState>>
│   │   │   └── AppState.kt                ← top-level auth + global state
│   │   ├── di/
│   │   │   ├── StorageModule.kt           ← SecureStorage (actual) Koin module
│   │   │   └── ViewModelModule.kt         ← all screen ViewModels as factory
│   │   └── utils/
│   │       ├── SvgUtils.kt                ← polylineToPath, getPointAtProgress
│   │       ├── DeltaDecoder.kt            ← merge(current, delta): VehicleRouteState
│   │       ├── IdempotencyKey.kt          ← UUID v4 generator
│   │       └── FrontendMetrics.kt         ← Performance API: FPS, render time, WS latency
```

---

## Phase 0 — Design System Bootstrap

**Goal**: establish design tokens, conventions, and shared Compose theme before any screen work.

### Deliverables
- [ ] `FleetTheme.kt` — MaterialTheme override with all color tokens, typography scale, shape tokens
- [ ] `StatusBadge.kt` — reusable pill for all 14 status/priority values
- [ ] `LoadingSkeleton.kt` — shimmer placeholder used by all async panels
- [ ] `ConfirmDialog.kt` — modal with title, message, confirm/cancel
- [ ] `KpiCard.kt` — bento-style summary card: icon, value, label, optional trend sparkline
- [ ] `PaginatedTable.kt` — generic table component: headers, rows, filter bar, load-more

### Design Token Checklist
- [ ] Status badge: color + label + icon for every variant (no color-only state signals, WCAG AA)
- [ ] Hover transitions set to `200ms ease` on interactive elements
- [ ] All touch targets `≥ 44px` tall
- [ ] Focus ring: `2px solid #3B82F6` on keyboard focus
- [ ] Empty state component with illustration slot + CTA button

### Verification
- All components render in isolation with each status variant
- Keyboard Tab order is logical on each component

---

## Phase 1 — Shared Core Infrastructure

**Goal**: all business-layer code that is platform-agnostic; web uses this unchanged.

### 1.0 Gradle Dependency Setup

All required dependencies are declared in [gradle/libs.versions.toml](../gradle/libs.versions.toml) and wired in [composeApp/build.gradle.kts](../composeApp/build.gradle.kts). This must be verified before writing any Phase 1 code.

**Versions:**
| Library | Version | Key artifact |
|---|---|---|
| Ktor Client | `3.1.2` | `io.ktor:ktor-client-core` (commonMain) |
| Koin | `4.0.2` | `io.insert-koin:koin-core` (commonMain) |
| Coil | `3.1.0` | `io.coil-kt.coil3:coil-compose` (commonMain) + `coil-network-ktor3` |
| kotlinx.serialization | `1.8.0` | `org.jetbrains.kotlinx:kotlinx-serialization-json` |
| kotlinx.coroutines | `1.10.1` | `org.jetbrains.kotlinx:kotlinx-coroutines-core` |
| kotlinx.datetime | `0.6.2` | `org.jetbrains.kotlinx:kotlinx-datetime` |

**Source-set dependency mapping:**
| Artifact | Source set |
|---|---|
| `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-websockets` | `commonMain` |
| `ktor-client-js` | `jsMain` + `wasmJsMain` |
| `ktor-client-okhttp` | `androidMain` |
| `ktor-client-darwin` | `iosMain` |
| `ktor-client-mock`, `koin-test`, `kotlinx-coroutines-test` | `commonTest` |
| `koin-core`, `koin-compose-viewmodel` | `commonMain` |
| `koin-android` | `androidMain` |

**Required plugin** (in `build.gradle.kts`):
```kotlin
alias(libs.plugins.kotlinSerialization)
```

### 1.1 API DTOs

One `@Serializable` data class per backend request/response type. Group by domain module.  
Field types must match backend contract exactly:
- Monetary amounts: `Long` (PHP whole units as stored in backend)
- Timestamps: `Long` (epoch ms) or `kotlinx.datetime.Instant`
- Enums: `@SerialName`-annotated, with `UNKNOWN` fallback for forward compatibility

### 1.2 `ApiResponse<T>` Envelope

```kotlin
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val requestId: String? = null
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val fieldErrors: List<FieldError> = emptyList()
)
```

### 1.3 `FleetApiClient`

- Ktor `HttpClient` in `commonMain`
- `Authorization: Bearer <token>` injected via `HttpClientPlugin`
- On `401`: clear token via `SecureStorage`, emit `AuthEvent.Unauthorized` on `SharedFlow`
- On `429`: emit `AuthEvent.RateLimited` — UI shows non-blocking snackbar
- Base URL injected via `expect/actual` `PlatformConfig`
- All calls return `Result<T>` — callers never see raw HTTP exceptions

### 1.4 Repositories (one per domain)

| Repository | Key methods |
|---|---|
| `AuthRepository` | `login()`, `register()`, `verifyEmail()` |
| `VehicleRepository` | `getAll()`, `getById()`, `create()`, `update()`, `delete()`, `updateState()`, `recordOdometer()` |
| `RentalRepository` | `getAll()`, `getById()`, `create()`, `activate()`, `complete()`, `cancel()` |
| `CustomerRepository` | `getAll()`, `getById()`, `create()` |
| `MaintenanceRepository` | `create()`, `getByVehicle()`, `start()`, `complete()`, `cancel()` |
| `AccountingRepository` | `createInvoice()`, `payInvoice()`, `getPayments()`, `getAccounts()`, `getPaymentMethods()` |
| `TrackingRepository` | `getRoutes()`, `postLocation()`, `getVehicleState()`, `getFleetStatus()`, `getHistory()` |

### 1.5 `FleetLiveClient` (WebSocket)

```kotlin
class FleetLiveClient(private val httpClient: HttpClient) {
    val connectionState: StateFlow<ConnectionState>
    // DISCONNECTED → CONNECTING → CONNECTED / ERROR
    suspend fun connect(token: String, onDelta: (VehicleStateDelta) -> Unit)
    fun disconnect()
    // Reconnect: 5s fixed delay on web
    // Ping frames every 30s; disconnect if Pong not received within 30s
}
```

### 1.6 `AppDependencyDispatcher`

Decodes JWT on login, reads `roles` claim, returns `UserFeatureSet`:
```kotlin
sealed interface UserFeatureSet {
    data object Backoffice : UserFeatureSet  // ADMIN, FLEET_MANAGER, CUSTOMER_SUPPORT, RENTAL_AGENT
    data object Driver     : UserFeatureSet
    data object Customer   : UserFeatureSet
    data object MultiRole  : UserFeatureSet  // prompt user to choose
}
```

### 1.7 Pagination State

```kotlin
data class PaginatedState<T>(
    val items: List<T> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true
)
```

Every list ViewModel exposes `loadMore()` that merges into `PaginatedState`.

### 1.8 `FieldValidator`

Rules must match backend exactly:

| Field         | Rule                                              |
|---------------|---------------------------------------------------|
| VIN           | Exactly 17 alphanumeric characters                |
| License plate | Non-blank                                         |
| License expiry | Must be a future date                            |
| Odometer      | New reading must be > last recorded reading       |
| Email         | RFC 5322 pattern                                  |
| Password      | Minimum 8 characters                             |

### 1.9 Koin Module Definitions

Define all Koin modules before writing any screen code. These modules are the single source of truth for object wiring.

```kotlin
// commonMain/di/NetworkModule.kt
val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) { json() }
            install(FleetAuthPlugin) { storage = get() }  // SecureStorage injected
        }
    }
    single { FleetApiClient(get()) }
}

// commonMain/di/RepositoryModule.kt
val repositoryModule = module {
    single { AuthRepository(get()) }
    single { VehicleRepository(get()) }
    single { RentalRepository(get()) }
    single { CustomerRepository(get()) }
    single { MaintenanceRepository(get()) }
    single { AccountingRepository(get()) }
    single { TrackingRepository(get()) }
}

// commonMain/di/TrackingModule.kt
val trackingModule = module {
    single { FleetLiveClient(get()) }
    single { FleetState(get()) }
}

// webMain/di/StorageModule.kt
val storageModule = module {
    single { SecureStorage() }   // webMain actual implementation
}

// webMain/di/ViewModelModule.kt
val viewModelModule = module {
    viewModel { DashboardViewModel(get(), get(), get(), get()) }
    viewModel { VehicleListViewModel(get()) }
    viewModel { VehicleDetailViewModel(get(), get()) }
    viewModel { RentalListViewModel(get()) }
    viewModel { RentalDetailViewModel(get(), get()) }
    viewModel { CustomerListViewModel(get()) }
    viewModel { CustomerDetailViewModel(get()) }
    viewModel { MaintenanceListViewModel(get()) }
    viewModel { MaintenanceDetailViewModel(get()) }
    viewModel { AccountingViewModel(get()) }
    viewModel { FleetTrackingViewModel(get(), get()) }
    viewModel { UsersViewModel(get()) }
}
```

All `get()` calls resolve from Koin's graph — no repository or ViewModel should ever be constructed with direct `new` / constructor calls in production code.

### Phase 1 Verification
- [ ] All DTO classes compile with `kotlinx.serialization`
- [ ] `FleetApiClient` round-trip tested against live backend (login + token store)
- [ ] 401 interception clears session and emits `Unauthorized` event
- [ ] `FieldValidator` unit tests pass for all rules and edge cases
- [ ] `startKoin { modules(...) }` loads without missing-binding errors at app startup

---

## Phase 2 — Auth & Navigation Shell

**Goal**: a working app shell that guards routes and handles login/logout.

### 2.0 Koin Initialization (`App.kt`)

The very first thing `main()` must do is start Koin before mounting any Compose tree:

```kotlin
// webMain/App.kt
fun main() {
    startKoin {
        modules(networkModule, repositoryModule, trackingModule, storageModule, viewModelModule)
    }
    onWasmReady {  // or BrowserViewportWindow for JS IR
        Window("Fleet Backoffice") {
            App()
        }
    }
}
```

`startKoin` must complete before any `koinViewModel()` or `get()` call or Koin will throw `NoBeanDefFoundException`.

### 2.1 Login Screen (`/login`)
- Email + password form
- Calls `POST /v1/users/login`
- On success: store JWT in `SecureStorage`, decode roles via `AppDependencyDispatcher`
- Backoffice roles (ADMIN, FLEET_MANAGER, CUSTOMER_SUPPORT, RENTAL_AGENT) → navigate to `/dashboard`
- Other roles → show "Access denied" error (this portal is backoffice-only)
- Form validation inline using `FieldValidator` before submission
- Disable submit while `UiState.Loading`

### 2.2 `Router` (hash-based for JS)

Full route table:
```
/login
/dashboard
/vehicles              /vehicles/:id          /vehicles/new
/rentals               /rentals/:id           /rentals/new
/customers             /customers/:id         /customers/new
/maintenance           /maintenance/:id       /maintenance/new
/accounting/invoices   /accounting/invoices/:id
/accounting/accounts
/accounting/payments
/tracking/map
/users                 /users/:id             (ADMIN only)
/profile
```

### 2.3 `RouteGuard`

- Reads `SecureStorage.getToken()` on each navigation event
- If null → redirect to `/login`
- Checks role against allowed roles for the route; if unauthorized → redirect to `/dashboard` with alert snackbar
- ADMIN-only routes: `/users`, `/users/:id`

### 2.4 `AppShell` Layout

Two-column layout: collapsible sidebar (240px) + main content area.
- **Sidebar**: fleet logo, nav items grouped by module, user avatar + name at bottom, logout button
- **TopBar**: current page title, breadcrumb for detail pages, notification slot
- Sidebar collapses to icon-only on < 1024px viewport
- Active route highlighted with `#1E40AF` left border + background tint

### Phase 2 Verification
- [ ] Login with valid credentials navigates to dashboard
- [ ] Login with wrong credentials shows inline error message
- [ ] Unauthenticated navigation to any protected route redirects to `/login`
- [ ] ADMIN sees "Users" nav item; non-ADMIN does not
- [ ] Logout clears sessionStorage and redirects to `/login`

---

## Phase 3 — Dashboard Screen

**Goal**: real-data summary visible immediately on login.

### Layout — Bento Grid (4 columns, 2 rows)

```
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Total       │ Active      │ Fleet Live  │ Scheduled   │
│ Vehicles    │ Rentals     │ Status      │ Maintenance │ ← KpiCard ×4
├─────────────┴─────────────┴─────────────┴─────────────┤
│                                                         │
│             Fleet Map Panel (SVG schematic)             │  ← 2×2 span
│                                                         │
├─────────────────────────────┬───────────────────────────┤
│  Recent Rentals (last 5)    │  Recent Maintenance (last 5) │
└─────────────────────────────┴───────────────────────────┘
```

### Data Sources
| Card | Endpoint | Notes |
|---|---|---|
| Total Vehicles | `GET /v1/vehicles` | count from response |
| Active Rentals | `GET /v1/rentals?limit=1` | filter `status=ACTIVE` count |
| Fleet Live Status | `GET /v1/tracking/fleet/status` | `totalVehicles`, `activeVehicles` |
| Scheduled Maintenance | maintenance list aggregation | count `status=SCHEDULED` |
| Fleet Map | `GET /v1/tracking/routes` + WS `/v1/fleet/live` | initial positions + live deltas |
| Recent Rentals | `GET /v1/rentals?limit=5` | |
| Recent Maintenance | maintenance list `?limit=5` | |

### Parallel Loading (kotlin-coroutines-expert)

```kotlin
suspend fun loadDashboard(): DashboardData = supervisorScope {
    val vehiclesDeferred  = async { vehicleRepo.getAll(limit = 1) }
    val rentalsDeferred   = async { rentalRepo.getAll(limit = 5) }
    val fleetDeferred     = async { trackingRepo.getFleetStatus() }
    val maintenanceDeferred = async { maintenanceRepo.getScheduled(limit = 5) }

    DashboardData(
        vehicles    = vehiclesDeferred.await().getOrNull(),
        rentals     = rentalsDeferred.await().getOrNull(),
        fleetStatus = fleetDeferred.await().getOrNull(),
        maintenance = maintenanceDeferred.await().getOrNull()
    )
}
```

Each `KpiCard` shows `LoadingSkeleton` until its individual deferred resolves.  
Fleet map panel also uses `LoadingSkeleton` until routes load.

### Phase 3 Verification
- [ ] All 4 KPI cards show real counts (not hardcoded)
- [ ] Fleet map renders at least one route path
- [ ] Recent lists populate from API data
- [ ] Any single API failure shows in-card error state without crashing others

---

## Phase 4 — Vehicles Module

### 4.1 Vehicles List (`/vehicles`)

**Table columns**: License Plate · Make / Model · Year · State badge · Mileage (km)  
**Filter bar**: State multi-select · Make text · Model text · Reset button  
**Pagination**: "Load More" button, enabled when `hasMore = true`  
**Actions**: "Add Vehicle" button (ADMIN / FLEET_MANAGER only) → `/vehicles/new`

### 4.2 Vehicle Detail (`/vehicles/:id`) — 5 tabs

| Tab | Content | API |
|---|---|---|
| **Info** | All fields: VIN, plate, make, model, year, color, mileage; inline edit form (ADMIN/FLEET_MANAGER) | `GET/PATCH /v1/vehicles/{id}` |
| **State** | Current state badge + valid transition button; transitions blocked if invalid | `PATCH /v1/vehicles/{id}/state` |
| **Odometer** | History list + "Record Reading" form; validates new > last | `POST /v1/vehicles/{id}/odometer` |
| **Maintenance** | Table of all jobs for this vehicle with status/priority badges | `GET /v1/maintenance/vehicle/{id}` |
| **Tracking History** | Paginated location list: timestamp, lat/lon, speed, heading | `GET /v1/tracking/vehicles/{id}/history` |

**State transition matrix** (button shown only when transition is valid):

| From | Button label | Target state |
|---|---|---|
| AVAILABLE | Send to Maintenance | MAINTENANCE |
| AVAILABLE | Retire Vehicle | RETIRED |
| MAINTENANCE | Mark Available | AVAILABLE |
| RENTED | (no manual transition) | — |

### 4.3 Create Vehicle (`/vehicles/new`)
- Form: VIN (17 chars enforced), plate, make, model, year, color
- VIN field uses `mono` font family
- Inline validation errors shown per field on blur

### Phase 4 Verification
- [ ] Vehicle list loads paginated from API
- [ ] State badge colors match design system tokens exactly
- [ ] Invalid state transitions are not offered as buttons
- [ ] VIN field rejects < 17 or > 17 characters before HTTP call
- [ ] Odometer rejects new reading ≤ last reading

---

## Phase 5 — Rentals Module

### 5.1 Rentals List (`/rentals`)
- Table: Rental # · Customer · Vehicle plate · Status badge · Start Date · End Date · Total (PHP)
- Filter: Status multi-select · Date range picker · Reset
- "New Rental" → `/rentals/new`

### 5.2 Rental Detail (`/rentals/:id`)
- Summary card: all fields
- **Transition buttons** (shown only when valid):

| Current status | Button | Action |
|---|---|---|
| RESERVED | Activate Rental | `POST /v1/rentals/{id}/activate` |
| RESERVED | Cancel Rental | `POST /v1/rentals/{id}/cancel` |
| ACTIVE | Complete Rental | `POST /v1/rentals/{id}/complete` |

- Complete Rental requires final odometer input (validated > last recorded)
- **Invoice section**: linked invoice status badge; "Pay Invoice" form (payment method selector from `GET /v1/accounting/payment-methods`)
- Pay action generates `Idempotency-Key: UUID-v4` header before dispatch

### 5.3 Create Rental (`/rentals/new`)
- Fields: customer selector, vehicle selector (only AVAILABLE vehicles), start date, end date, daily rate
- Dates validated: start < end; start ≥ today

### Phase 5 Verification
- [ ] ACTIVE rental does not show Activate button
- [ ] COMPLETED rental shows no transition buttons
- [ ] Pay invoice generates unique `Idempotency-Key` on each attempt
- [ ] Cancel shows `ConfirmDialog` before dispatching

---

## Phase 6 — Customers Module

### 6.1 Customers List (`/customers`)
- Table: Name · Email · Phone · License # · License Expiry · Active badge
- "Add Customer" → `/customers/new`
- Inline deactivate toggle (calls `PATCH` on customer record)

### 6.2 Customer Detail (`/customers/:id`)
- Read-only view of all fields
- Linked rentals history (calls `GET /v1/rentals` filtered by customer)
- Linked payments history

### 6.3 Create Customer (`/customers/new`)
- Fields: user ID (linked to existing user), driver license number, license expiry
- License expiry validated as future date only

### Phase 6 Verification
- [ ] Expired license shown with `EF4444` color warning on list
- [ ] Inactive customers have visual distinction on list

---

## Phase 10 — Maintenance Module

### 7.1 Maintenance List (`/maintenance`)
- Table: Job # · Vehicle · Type badge · Priority badge · Status badge · Scheduled Date · Est. Cost
- Filter: Priority · Type · Status · Vehicle
- "Schedule Job" → `/maintenance/new`

### 7.2 Maintenance Detail (`/maintenance/:id`)
- Job info card
- **Transition buttons**:

| Current | Button | Action |
|---|---|---|
| SCHEDULED | Start Job | `POST /v1/maintenance/{id}/start` |
| SCHEDULED | Cancel Job | `POST /v1/maintenance/{id}/cancel` |
| IN_PROGRESS | Complete Job | `POST /v1/maintenance/{id}/complete` |

- Complete Job: requires `laborCost` and `partsCost` inputs
- Cost summary card: labor + parts + total display

### 7.3 Schedule Maintenance (`/maintenance/new`)
- Fields: vehicle selector, job type, priority, scheduled date, estimated cost, description

### Phase 10 Verification
- [ ] URGENT priority badge renders with `EF4444`
- [ ] Complete Job form validates `laborCost` and `partsCost` are positive numbers
- [ ] Cancel shows `ConfirmDialog`

---

## Phase 8 — Accounting Module

### 8.1 Invoices (`/accounting/invoices`)
- Table: Invoice # · Customer · Rental · Status badge · Amount (PHP) · Due Date
- "Create Invoice" form: linked rental selector, due date

### 8.2 Invoice Detail (`/accounting/invoices/:id`)
- Invoice summary
- **Pay** action: opens modal with payment method selector (from `GET /v1/accounting/payment-methods`)
- Generates fresh `Idempotency-Key: UUID-v4` per attempt before sending `POST /v1/accounting/invoices/{id}/pay`
- OVERDUE invoices highlighted with `EF4444` background tint on the due date field

### 8.3 Chart of Accounts (`/accounting/accounts`)
- Collapsible tree grouped by `AccountType`: ASSET · LIABILITY · EQUITY · REVENUE · EXPENSE
- Each row: account code (mono font) · name · type · balance
- Balance negative values shown in `EF4444`

### 8.4 Payments (`/accounting/payments`)
- Table: Invoice # · Customer · Amount (PHP) · Payment Method · Date
- Filter: date range · payment method

### Phase 8 Verification
- [ ] PHP currency formatted as `₱ X,XXX.XX` throughout
- [ ] Chart of accounts tree collapses/expands per `AccountType`
- [ ] Idempotency-Key is generated client-side (UUID v4); same invoice cannot double-pay even on retry

---

## Phase 9 — Fleet Tracking Map

> **Consolidated from**: `docs/web-schematic-visualization.md`  
> This is the most technically complex screen and has pre-existing detailed specifications that are adopted in full here.

### 9.1 Architecture

```
FleetState (MutableStateFlow map)
    ↑
    └── FleetLiveClient (WS /v1/fleet/live)
            ↓ VehicleStateDelta
        DeltaDecoder.merge(current, delta) → VehicleRouteState
            ↓
FleetMap (SVG canvas) ← recomposes on state change
    ├── RouteLayer (one <path> per route from GET /v1/tracking/routes)
    └── VehicleIcon (one <polygon> per vehicle, 500ms animated tween)
```

### 9.2 `SvgUtils`

```kotlin
object SvgUtils {
    // Convert PostGIS LineString → SVG path data string
    fun polylineToPath(lineString: String): String

    // Get (x, y) at progress 0.0..1.0 along a polyline
    fun getPointAtProgress(lineString: String, progress: Double): Point
}

data class Point(val x: Double, val y: Double)
```

### 9.3 `DeltaDecoder`

```kotlin
object DeltaDecoder {
    // Fields present in delta overwrite current state; absent fields retain their values
    fun merge(current: VehicleRouteState?, delta: VehicleStateDelta): VehicleRouteState
}
```

### 9.4 `FleetState`

```kotlin
class FleetState(private val wsClient: FleetLiveClient) {
    private val _vehicles = MutableStateFlow<Map<VehicleId, VehicleRouteState>>(emptyMap())
    val vehicles: StateFlow<Map<VehicleId, VehicleRouteState>> = _vehicles.asStateFlow()

    private val _routes = MutableStateFlow<Map<RouteId, RouteDto>>(emptyMap())
    val routes: StateFlow<Map<RouteId, RouteDto>> = _routes.asStateFlow()

    private fun updateVehicle(delta: VehicleStateDelta) {
        _vehicles.update { current ->
            val existing = current[delta.vehicleId]
            current + (delta.vehicleId to DeltaDecoder.merge(existing, delta))
        }
    }
}
```

### 9.5 `FleetMap` SVG Canvas

- Root `<svg viewBox="0 0 1000 1000">` with `map-bg` (#0F172A) background
- `RouteLayer`: one `<path>` per route, `stroke: map-route`, `stroke-width: 2`, `fill: none`
- `VehicleIcon`: `<polygon>` directional marker (arrow shape), color from vehicle status token, 500ms `animateFloatAsState` tween on position change, `rotate(bearing)` transform
- Right sidebar: vehicle list sorted by status → click to highlight/pan to marker
- Selection info panel: plate, speed, heading, route progress %, last update timestamp
- Bottom status bar: WS `ConnectionState` indicator — Connected (green dot) / Reconnecting (amber pulse) / Offline (red dot)

### 9.6 WebSocket Connection Rules

| Event | Behaviour |
|---|---|
| Connect | `CONNECTING` state, connect to `WS /v1/fleet/live` with `Authorization: Bearer {token}` |
| Connected | `CONNECTED` state; start `sendHeartbeat()` coroutine (Ping every 30s) |
| Pong timeout (30s) | Disconnect, emit `ERROR` state |
| Disconnected / error | Schedule reconnect with **5s fixed delay** |
| Page unload | `disconnect()` cleanly |

### 9.7 FrontendMetrics

```kotlin
object FrontendMetrics {
    fun recordRenderTime(vehicleId: String, durationMs: Double)
    fun recordWebSocketLatency(latencyMs: Double)
    fun recordAnimationFps(fps: Double)
}
```

Metrics written to `window.performance` (Performance API). Visible in browser DevTools → Performance panel.

### Performance Targets (from web-schematic-visualization.md)
| Metric | Target | Critical threshold |
|---|---|---|
| FPS (50 vehicles) | ≥ 55 fps | < 30 fps |
| Render time per frame | < 16ms | > 33ms |
| WS message → render | < 100ms | > 500ms |
| Time to first render | < 2s | > 5s |
| Memory usage | < 100MB | > 500MB |

### Phase 9 Verification
- [ ] Routes render from `GET /v1/tracking/routes` as SVG paths
- [ ] Vehicles animate with 500ms tween on position change
- [ ] WebSocket reconnects automatically after 5s on disconnect
- [ ] Ping/Pong heartbeat running; disconnect on pong timeout
- [ ] Clicking a sidebar vehicle row highlights and centers its marker
- [ ] `ConnectionState` indicator visible in status bar

---

## Phase 7 — Users Management (ADMIN only)

### 10.1 Users List (`/users`)
- Table: Name · Email · Roles · Verified · Active
- Route guard: ADMIN role only; other roles redirected to `/dashboard`

### 10.2 User Detail (`/users/:id`)
- Profile fields (read-only)
- **Role assignment**: multi-select chip UI → `POST /v1/users/{id}/roles`
  - Available roles: `ADMIN`, `FLEET_MANAGER`, `CUSTOMER_SUPPORT`, `RENTAL_AGENT`, `DRIVER`, `CUSTOMER`
  - Selected roles shown as dismissible chips
- **Delete user**: `ConfirmDialog` with explicit confirmation text → `DELETE /v1/users/{id}`

### Phase 7 Verification
- [ ] Non-ADMIN users cannot see the "Users" sidebar item
- [ ] Non-ADMIN direct navigation to `/users` redirects to `/dashboard` with snackbar
- [ ] Role chip UI updates optimistically; reverts on API error
- [ ] Delete requires two-step confirmation

---

## Phase 11 — Testing & Quality Assurance

### 11.1 Unit Tests (commonTest)

| Test target | Assertions |
|---|---|
| `DeltaDecoder` | `merge(null, delta)` creates full state; `merge(existing, partial)` retains absent fields |
| `SvgUtils.polylineToPath` | Empty string returns `""`; valid linestring returns valid SVG path starting with `M` |
| `SvgUtils.getPointAtProgress` | `progress=0.0` returns first coord; `progress=1.0` returns last coord |
| `FieldValidator` | All 6 rules pass valid input and reject invalid input with correct error messages |
| `ApiResponse<T>` deserializer | `success=false` maps to typed error; `fieldErrors` array populated |

All tests use `runTest` + injected `TestDispatcher` (kotlin-coroutines-expert rule).

### 11.1.1 Koin DI Tests

Use `koin-test` to verify the production Koin graph is complete and correctly wired:

```kotlin
class KoinModuleTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(networkModule, repositoryModule, trackingModule, storageModule, viewModelModule)
    }

    @Test
    fun `all production modules load without missing bindings`() {
        checkModules {
            // Koin will throw if any declared dependency is unsatisfied
        }
    }
}
```

For ViewModel unit tests, override the production module with a test double module:

```kotlin
val fakeRepositoryModule = module {
    single<VehicleRepository> { FakeVehicleRepository() }
}

// In test setup:
startKoin { modules(fakeRepositoryModule, viewModelModule) }
// In test teardown:
stopKoin()
```

### 11.2 Integration Tests (webTest)

- `FleetLiveClient`: connect → receive delta → `FleetState` updated → disconnect → reconnect after 5s
- Mock WS server using `MockEngine` from Ktor test utilities

### 11.3 Performance Tests

```kotlin
class RenderPerformanceTest {
    @Test
    fun `fleet map renders 50 vehicles within 16ms per frame`() {
        // Simulate 50 vehicle state updates; measure render time via FrontendMetrics
        repeat(50) { fleetState.applyDelta(mockDelta(it)) }
        assertTrue(FrontendMetrics.lastRenderTime < 16.0)
    }
}
```

### 11.4 E2E Tests (Playwright)

Key flows:
1. Login → Dashboard loads KPI cards with real data
2. Create Vehicle → appears in list
3. Vehicle state transition → state badge updates in real time
4. Pay Invoice → Idempotency-Key sent in request header
5. Fleet Tracking Map → WebSocket connects → vehicle markers appear

### Coverage Target
- Unit + integration: **≥ 75%** for all `commonMain` and `webMain` modules
- P11 must not be skipped to reach production deployment

---

## Phase 12 — Performance & Production Build

### Bundle Targets
- **Total JS bundle**: < 500KB gzipped
- **Time to interactive**: < 3s on a 4G connection
- **First contentful paint**: < 1.5s

### Optimisation Checklist
- [ ] Dead code elimination via Kotlin/JS IR compiler (`--mode=PRODUCTION`)
- [ ] Webpack tree-shaking enabled
- [ ] All images/icons as inline SVG or Compose vector drawables (no PNG assets over 8KB)
- [ ] Google Fonts (Inter) loaded with `display=swap` and font subsetting on Latin charset only
- [ ] Lazy-load fleet tracking screen (dynamic import) — heaviest SVG computation deferred
- [ ] `FrontendMetrics` calls stripped in production build via `BuildConfig` flag

### Production Build Commands

```powershell
# Production webpack bundle
.\gradlew.bat :composeApp:jsBrowserProductionWebpack

# Bundle size report
.\gradlew.bat :composeApp:jsBrowserProductionWebpack --info | Select-String "bundle size"

# Run all tests before build
.\gradlew.bat :composeApp:jsTest
```

### Security Checklist (OWASP)
- [ ] JWT stored only in `sessionStorage` — never `localStorage`, cookies, or URL params
- [ ] All API calls over HTTPS — HTTP base URL blocked at `PlatformConfig` level
- [ ] `Idempotency-Key` generated client-side as UUID v4 before every payment POST
- [ ] Role guard enforced both at sidebar (hidden items) and at route level (redirect on direct access)
- [ ] 401 response handler: clears token + forces navigation to `/login`
- [ ] Content Security Policy header set on the hosting server
- [ ] No sensitive data (vehicle coordinates, user PII) logged to browser console in production

---

## Implementation Sequence Summary

> Status for each phase is tracked in its dedicated file under `docs/phases/`. Update the `Status` field in the phase file as work progresses.

| Phase | Scope | Status | Prerequisite |
|---|---|---|---|
| [0 — Design System](./phases/phase-00-design-system.md) | Design tokens, shared components | `NOT STARTED` | — |
| [1 — Shared Core](./phases/phase-01-shared-core.md) | DTOs, Client, Repos, WS, Validation, Koin modules | `NOT STARTED` | Phase 0 |
| [2 — Auth + Navigation](./phases/phase-02-auth-navigation.md) | Login, Router, RouteGuard, AppShell | `NOT STARTED` | Phase 1 |
| [3 — Dashboard](./phases/phase-03-dashboard.md) | Bento grid, KPI cards, Fleet map panel | `NOT STARTED` | Phase 2 |
| [4 — Vehicles](./phases/phase-04-vehicles.md) | List, detail (5 tabs), create | `NOT STARTED` | Phase 3 |
| [5 — Rentals](./phases/phase-05-rentals.md) | List, detail, transitions, invoicing | `NOT STARTED` | Phase 4 |
| [6 — Customers](./phases/phase-06-customers.md) | List, detail, create | `NOT STARTED` | Phase 2 |
| [7 — Users Management](./phases/phase-07-users-management.md) | ADMIN-only user list + role assignment | `NOT STARTED` | Phase 2 |
| [8 — Accounting](./phases/phase-08-accounting.md) | Invoices, CoA tree, payments | `NOT STARTED` | Phase 5 |
| [9 — Fleet Tracking Map](./phases/phase-09-fleet-tracking-map.md) | SVG canvas, WebSocket, animations | `NOT STARTED` | Phase 1 + Phase 3 |
| [10 — Maintenance](./phases/phase-10-maintenance.md) | List, detail, transitions, scheduling | `NOT STARTED` | Phase 4 |
| [11 — Testing & QA](./phases/phase-11-testing-qa.md) | Unit, integration, E2E, ≥ 75% coverage | `NOT STARTED` | Phases 4–10 |
| [12 — Performance & Build](./phases/phase-12-performance-production.md) | Bundle optimisation, security checklist | `NOT STARTED` | Phase 11 |
| [13 — System Overview](./phases/phase-13-system-overview.md) | Onboarding demo: architecture, deployment, mobile app cards | `NOT STARTED` | Phase 2 |

---

## References

- [kotlin-multiplatform-prompt.md](./kotlin-multiplatform-prompt.md) — full feature and API specification
- [web-schematic-visualization.md](./web-schematic-visualization.md) — SVG map deep-dive (Phase 9 source)
- [kotlin-coroutines-expert SKILL](./kotlin-coroutines-expert/SKILL.md) — coroutine patterns applied throughout
- [kotlin-specialist SKILL](./kotlin-specialist/SKILL.md) — KMP architecture, sealed classes, Flow, expect/actual
- [ui-ux-pro-max PROMPT](../.github/prompts/ui-ux-pro-max/PROMPT.md) — design system foundation (colors, typography, styles)
- Backend API: Ktor + PostgreSQL + Redis (live — do not modify)
