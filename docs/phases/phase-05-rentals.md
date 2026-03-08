# Phase 5 — Rentals Module

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 4
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## 5.1 Rentals List (`/rentals`)

- [x] Table: Rental # · Customer · Vehicle plate · Status badge · Start Date · End Date · Total (PHP)
- [x] Filter: Status filter chips (null + RESERVED/ACTIVE/COMPLETED/CANCELLED)
- [x] "New Rental" → `/rentals/new`

## 5.2 Rental Detail (`/rentals/:id`)

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

## 5.3 Create Rental (`/rentals/new`)

- [x] Fields: customer selector, vehicle selector (AVAILABLE only), start date, end date, daily rate
- [x] Dates validated: start < end; start ≥ today

## Caching Behavior

> `RentalRepositoryImpl` · TTL **30 s** | `AccountingRepositoryImpl` (invoices) · TTL **60 s** · see Phase 1.10.

| Action | Cache effect |
|---|---|
| Open Rentals list (warm) | Returns from `RentalRepository` listCache — no network |
| Explicit refresh | `forceRefresh = true` on `getRentals` |
| Create / Cancel / Complete rental | `RentalRepository.listCache.clear()` |
| Pay Invoice | `AccountingRepository.invoiceCache.clear()` + `paymentCache.clear()` |
| Payment Methods selector | `getPaymentMethods()` — not cached (lightweight, always fresh) |

`RentalsViewModel` should expose `isRefreshing: StateFlow<Boolean>` for background-refresh UX.

## Use Cases (Clean Architecture — Phase 1.11)

> `RentalsViewModel` injects **8 use cases** from `domain/usecase/rental/RentalUseCases.kt`. No repository is injected directly into the ViewModel.

| Use Case | Notes |
|---|---|
| `GetRentalsUseCase` | Paginated list with optional `status` filter |
| `GetRentalUseCase` | Single rental by ID |
| `CreateRentalUseCase` | Creates a new `RESERVED` rental |
| `ActivateRentalUseCase` | `RESERVED → ACTIVE` |
| `CancelRentalUseCase` | `RESERVED → CANCELLED` |
| `CompleteRentalUseCase` | `ACTIVE → COMPLETED` with final odometer |
| `GetPaymentMethodsUseCase` | Dropdown data for the Pay Invoice form |
| `PayInvoiceUseCase` | UUID idempotency key generated **inside** the use case — never in the ViewModel |

## Verification

- [x] ACTIVE rental does not show Activate button
- [x] COMPLETED rental shows no transition buttons
- [x] Pay invoice generates unique `Idempotency-Key` on each submission
- [x] Cancel shows `ConfirmDialog` before dispatching
- [x] Cancelling a rental from detail view invalidates list cache (back-nav shows updated status badge)
