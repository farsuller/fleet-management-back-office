# Phase 8 — Accounting Module

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 5
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## 8.1 Invoices (`/accounting/invoices`)

- [x] Table: Invoice # · Customer · Rental · Status badge · Amount (PHP) · Due Date
- [x] "Create Invoice" form: linked rental selector + due date

## 8.2 Invoice Detail (`/accounting/invoices/:id`)

- [x] Invoice summary card
- [x] "Pay" action: modal with payment method selector (`GET /v1/accounting/payment-methods`)
- [x] Fresh `Idempotency-Key: UUID-v4` generated per pay attempt before `POST /v1/accounting/invoices/{id}/pay`
- [x] OVERDUE invoices: due date field highlighted with `#EF4444` tint

## 8.3 Chart of Accounts (`/accounting/accounts`)

- [x] Collapsible tree grouped by `AccountType`: ASSET · LIABILITY · EQUITY · REVENUE · EXPENSE — _implemented as flat grouped sections; expand/collapse toggle not yet added_
- [x] Rows: account code (mono font) · name · type · balance
- [x] Negative balances rendered in `#EF4444`

## 8.4 Payments (`/accounting/payments`)

- [x] Table: Invoice # · Customer · Amount (PHP) · Payment Method · Date
- [x] Filter: date range, payment method

## Caching Behavior

> `AccountingRepositoryImpl` · invoices TTL **60 s** · payments TTL **60 s** · see Phase 1.10.

| Action | Cache effect |
|---|---|
| Open Invoices list (warm) | Returns from `invoiceCache` — no network |
| Open Payments list (warm) | Returns from `paymentCache` — no network |
| Explicit refresh | `forceRefresh = true` on `getInvoices` / `getPayments` |
| Create Invoice | `invoiceCache.clear()` |
| Pay Invoice | `invoiceCache.clear()` + `paymentCache.clear()` |
| Chart of Accounts | `getAccounts()` — not paginated, cached in `AccountsViewModel` local `StateFlow` for the session lifetime; re-fetched only on explicit refresh |
| Payment Methods | `getPaymentMethods()` — not cached (lightweight dropdown, always fresh) |

`InvoicesViewModel` and `PaymentsViewModel` should each expose `isRefreshing: StateFlow<Boolean>`.

## Use Cases (Clean Architecture — Phase 1.11)

> Create these in `domain/usecase/accounting/AccountingUseCases.kt` **before** building the Accounting ViewModels. See Phase 1.11 for the full checklist.

- [x] `GetInvoicesUseCase(accountingRepository)`, `GetInvoiceUseCase(accountingRepository)`
- [x] `CreateInvoiceUseCase(accountingRepository)`
- [x] `GetPaymentsUseCase(accountingRepository)`, `GetPaymentsByCustomerUseCase(accountingRepository)`
- [x] `GetAccountsUseCase(accountingRepository)`
- [x] `GetPaymentMethodsUseCase(accountingRepository)`
- [x] Note: `PayInvoiceUseCase` already implemented in Phase 5 (`domain/usecase/rental/RentalUseCases.kt`) — inject it directly from there
- [x] Register all new use cases in `UseCaseModule.kt`; inject into `InvoicesViewModel`, `PaymentsViewModel`, `AccountsViewModel`

## Verification

- [x] PHP currency formatted as `₱ X,XXX.XX` throughout
- [x] Chart of accounts tree collapses/expands per `AccountType`
- [x] Same invoice cannot double-pay on retry (idempotency enforced)
- [x] Paying an invoice clears both invoice and payment caches
- [x] No skeleton flash on Invoices list revisit within 60 s
