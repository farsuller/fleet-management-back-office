# Phase 6 — Customers Module

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 2
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## 6.1 Customers List (`/customers`)

- [x] Table: Name · Email · Phone · License # · License Expiry · Active badge
- [x] "Add Customer" → `/customers/new`
- [x] Inline deactivate toggle (`PATCH /v1/customers/{id}/deactivate` — toggles `isActive`)

## 6.2 Customer Detail (`/customers/:id`)

- [x] Read-only view of all customer fields
- [x] Linked rentals history (`GET /v1/rentals` filtered by customer)
- [x] Linked payments history (`GET /v1/accounting/payments/customer/{id}`)

## 6.3 Create Customer (`/customers/new`)

- [x] Fields: user ID selector, driver license number, license expiry
- [x] License expiry validated as future date only

## Caching Behavior

> `CustomerRepositoryImpl` · TTL **120 s** · see Phase 1.10.

| Action | Cache effect |
|---|---|
| Open Customers list (warm) | Returns from `listCache` — no network |
| Explicit refresh | `forceRefresh = true` on `getCustomers` |
| Create customer | `listCache.clear()` |
| Deactivate toggle (`PATCH`) | `listCache.clear()` — next list fetch reflects updated `isActive` |
| Customer Detail → rental/payment history | Uses `RentalRepository` (30 s) scoped by customer ID as key segment |

`CustomersViewModel` should expose `isRefreshing: StateFlow<Boolean>`.

## Use Cases (Clean Architecture — Phase 1.11)

> `CustomersViewModel` injects **6 use cases** from `domain/usecase/customer/CustomerUseCases.kt`. No repository is injected directly into the ViewModel.

| Use Case | Notes |
|---|---|
| `GetCustomersUseCase` | Paginated customer list |
| `GetCustomerUseCase` | Single customer by ID |
| `CreateCustomerUseCase` | New customer registration |
| `DeactivateCustomerUseCase` | Toggles `isActive` via `PATCH /v1/customers/{id}/deactivate` |
| `GetCustomerRentalsUseCase` | Fetches all rentals then applies client-side filter: `it.customerId == customerId` |
| `GetCustomerPaymentsUseCase` | `GET /v1/accounting/payments/customer/{id}` |

## Verification

- [x] Expired license shown with `#EF4444` color warning on list
- [x] Inactive customers have visible distinction on list (Switch toggle reflects state)
- [x] Creating a customer and navigating back to list shows new entry (cache invalidated)
- [x] No skeleton flash on list revisit within 120 s
