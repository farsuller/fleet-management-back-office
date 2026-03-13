# Android Customer App - Backend-Aligned Feature Breakdown

## Status
- Overall: Ready for Implementation
- Scope: Customer-only mobile experience (separate from driver app concerns)

## Purpose
Define a complete customer-mobile feature breakdown based on existing backend APIs and current fleet-management implementation docs.

## Backend Contract (Customer-Relevant)

### Authentication
- `POST /v1/users/register`
- `POST /v1/users/login`
- `GET /v1/auth/verify?token=...`

### Profile and Customer Records
- `GET /v1/users/{id}`
- `PATCH /v1/users/{id}`
- `GET /v1/customers/{id}`
- `POST /v1/customers`

### Vehicle Discovery
- `GET /v1/vehicles`
- `GET /v1/vehicles/{id}`

### Rental Lifecycle
- `GET /v1/rentals`
- `POST /v1/rentals`
- `GET /v1/rentals/{id}`
- `POST /v1/rentals/{id}/activate`
- `POST /v1/rentals/{id}/complete`
- `POST /v1/rentals/{id}/cancel`

### Tracking and Trip Visibility
- `GET /v1/tracking/vehicles/{vehicleId}/state`
- `GET /v1/tracking/vehicles/{vehicleId}/history?limit=&offset=`
- `GET /v1/tracking/routes/active`

### Payments and Accounting Views
- `POST /v1/accounting/invoices/{id}/pay` (requires `Idempotency-Key`)
- `GET /v1/accounting/payments/customer/{id}`
- `GET /v1/accounting/payment-methods`

## Customer Feature Breakdown

### 1) Account and Access
- Register account and verify email.
- Login/logout with persistent secure session.
- Role-aware dispatch to customer home.
- Edit personal profile and contact details.

### 2) Vehicle Search and Selection
- Browse available vehicles with filters.
- Vehicle detail with availability state and pricing context.
- Save preferred vehicle options for quick re-booking.

### 3) Booking and Reservation
- Create rental reservations.
- See status progression: `RESERVED -> ACTIVE -> COMPLETED/CANCELLED`.
- Cancel reservation with clear policy messaging.

### 4) Active Trip Tracking
- Poll current assigned vehicle state.
- Show speed, heading, route progress, and freshness timestamp.
- Render assigned route path from route catalog where applicable.
- Display fallback states for delayed/no telemetry.

### 5) Trip History
- Paginated timeline of historical tracking events.
- Rental history with vehicle and period details.
- Quick access to previous completed trips.

### 6) Payments and Invoices
- Pay invoice with idempotent payment submission.
- Payment method selection and validation.
- Customer payment history view.

### 7) Notifications and Support
- Reservation/rental status updates.
- Payment result notifications.
- Incident/help requests routed to support workflows.

## Customer App Architecture (Android-Only, Separate Repository)

### Core Platform
- DTOs and API envelopes (`ApiResponse<T>`) in Android modules.
- Repositories/use cases for auth, vehicles, rentals, tracking read, accounting read.
- Validation and UI-state models.
- UI screens and navigation.
- Push notification integration.
- Secure token storage.
- Local cache for recent rentals/history.

### Applied Modular Architecture (Customer App Repository)

Customer app module layout (separate repository):

```text
:app:customer
	- application init, DI, root nav host

:feature:auth
	- register/login/verify screens and flows

:feature:customer-profile
	- profile view/edit, account preferences

:feature:vehicle-catalog
	- browse/search/filter vehicle inventory

:feature:rental-booking
	- create/cancel/activate/complete rental lifecycle UI

:feature:trip-tracking
	- active trip status, route visibility, stale-state handling

:feature:payments
	- invoice payment, methods, payment history

:feature:notifications
	- in-app status alerts and support events

:core:api-contracts
	- DTOs, envelopes, and endpoint models owned by this Android repository

:core:network
	- Ktor client + auth + retries + error mapping

:core:database
	- lightweight cache for rentals/history/session snapshots

:core:navigation
	- navigation graph contracts

:core:ui
	- reusable customer design primitives
```

Dependency direction:

```text
presentation -> domain -> data -> core
```

Repository boundary guidance:
- Customer repo is Android-only and owns its API contract layer for customer-facing endpoints.
- Driver-only platform modules (sensors, geofence, foreground service, WorkManager telemetry queue) are intentionally excluded from customer repo.
- Contract changes are validated in CI with integration and schema-compatibility tests.

## API and Reliability Checklist
- [ ] JWT is attached to all protected calls.
- [ ] `ApiResponse<T>` parsing handles both success and error envelopes.
- [ ] Rental actions use optimistic UI with rollback on failure.
- [ ] Payment call sends stable `Idempotency-Key` across retries.
- [ ] Tracking poll interval is adaptive to app lifecycle.
- [ ] History lists use pagination (`limit`/`offset`).
- [ ] Rate-limit and transient errors are surfaced with retry UX.
- [ ] Request IDs are logged for support/debug.

## UX State Matrix
- `AUTHENTICATED` / `UNAUTHENTICATED`
- `NO_ACTIVE_RENTAL` / `RESERVED` / `ACTIVE` / `COMPLETED`
- `TRACKING_LIVE` / `TRACKING_STALE` / `TRACKING_UNAVAILABLE`
- `PAYMENT_IDLE` / `PAYMENT_IN_PROGRESS` / `PAYMENT_SUCCESS` / `PAYMENT_FAILED`

## Definition of Done
- [ ] Customer can register/login and maintain session.
- [ ] Customer can browse vehicles and create/cancel rental.
- [ ] Active rental tracking screen works with backend state endpoint.
- [ ] Rental and tracking history screens are paginated and stable.
- [ ] Customer payment flow is idempotent and auditable.
- [ ] Error handling is user-friendly and request IDs are logged.

## Implementation Bill of Materials (Customer)

### Core Dependencies
- Ktor Client + `kotlinx.serialization` for backend integration.
- Koin DI for feature-level dependency composition.
- Room for local cache (catalog, rental history, tracking snapshots).
- Coroutines + Flow for state updates and polling orchestration.
- Android Keystore (`KeyStore`/`KeyGenerator`/`Cipher`) + DataStore for secure token/session persistence.
- WorkManager for durable background retry/sync jobs (network-dependent work only).

### UX Dependencies
- Coil for vehicle media and profile assets.
- Vector icon system through `core:ui` for consistent status/icon rendering.

### Module Ownership
- `:core:api-contracts`: DTOs, `ApiResponse<T>`, endpoint definitions, and validation contracts.
- Android platform modules: cache adapters, notifications, secure storage, and Android bindings.
- `feature:*`: auth, catalog, booking, trip tracking, payments, notifications.
- `core:*`: network, database, navigation, and UI primitives.

### Test Stack
- `commonTest`: shared business rules and mappers.
- `androidUnitTest`: reducers/viewmodels, pagination, cache invalidation, payment idempotency behavior.
- `androidInstrumentedTest`: auth flow, booking-to-payment journey, tracking state UX.

### Caching Policy
- Customer app uses read-heavy caching with explicit stale-state indicators.
- Cache invalidation triggers on booking/payment mutations.
- Paginated history must preserve cursor/offset continuity.

### Efficient WorkManager Usage (Customer App)
- Use WorkManager only for deferrable, guaranteed background work (retry payment status sync, deferred history refresh, queued mutation replay).
- Do not use WorkManager for immediate UI actions; execute direct calls first, then enqueue recovery work only on failure/offline conditions.
- Apply network constraints (`NetworkType.CONNECTED`) so jobs run only when delivery can succeed.
- Use unique work names per workflow (`payment-retry`, `history-sync`) to prevent duplicate workers and wasted battery.
- Use exponential backoff and capped retry windows to avoid aggressive wakeups.
- Keep payloads small and idempotent (reuse `Idempotency-Key` for retried payment operations).
- Cancel obsolete jobs when user logs out or rental/payment context is no longer active.

## References
- `docs/frontend-implementations/android-implementations/android-driver-app.md`
- `docs/frontend-implementations/android-implementations/android-feature-breakdown.md`
- `src/main/kotlin/com/solodev/fleet/modules/tracking/infrastructure/http/TrackingRoutes.kt`
