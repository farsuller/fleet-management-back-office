# Phase 2 — Auth & Navigation Shell

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 1
> [← Back to master plan](../web-backoffice-implementation-plan.md)

**Goal**: working app shell with JWT auth, hash-based routing, role guards, and the sidebar/topbar layout.

---

## 2.0 Koin Initialization

- [ ] `startKoin { modules(...) }` is the first call in `main()` before any Compose tree mounts
- [ ] App starts without `NoBeanDefFoundException`

## 2.1 Login Screen (`/login`)

- [ ] Email + password form renders
- [ ] Calls `POST /v1/users/login`
- [ ] JWT stored in `SecureStorage` on success
- [ ] Roles decoded via `AppDependencyDispatcher`
- [ ] Backoffice roles (ADMIN, FLEET_MANAGER, CUSTOMER_SUPPORT, RENTAL_AGENT) → `/dashboard`
- [ ] Non-backoffice roles → "Access denied" error displayed
- [ ] Inline field validation via `FieldValidator` before HTTP call
- [ ] Submit disabled while `UiState.Loading`

## 2.2 `Router` (hash-based)

- [ ] `/login`
- [ ] `/dashboard`
- [ ] `/vehicles`, `/vehicles/:id`, `/vehicles/new`
- [ ] `/rentals`, `/rentals/:id`, `/rentals/new`
- [ ] `/customers`, `/customers/:id`, `/customers/new`
- [ ] `/maintenance`, `/maintenance/:id`, `/maintenance/new`
- [ ] `/accounting/invoices`, `/accounting/invoices/:id`
- [ ] `/accounting/accounts`
- [ ] `/accounting/payments`
- [ ] `/tracking/map`
- [ ] `/users`, `/users/:id` (ADMIN only)
- [ ] `/profile`

## 2.3 `RouteGuard`

- [ ] Token check on every navigation event
- [ ] Null token → `/login`
- [ ] Insufficient role → `/dashboard` + snackbar
- [ ] ADMIN restriction enforced on `/users` routes

## 2.4 `AppShell`

- [ ] Sidebar 240px, collapses to icon-only at `< 1024px`
- [ ] Fleet logo, grouped nav items, user avatar + name, logout button
- [ ] Active route: `#1E40AF` left border + background tint
- [ ] TopBar: page title, breadcrumb (detail pages), notification slot

## Verification

- [ ] Valid login → navigates to dashboard
- [ ] Invalid login → inline error (no navigation)
- [ ] Unauthenticated deep link → redirects to `/login`
- [ ] ADMIN sees "Users" nav item; non-ADMIN does not
- [ ] Logout clears `sessionStorage` and redirects to `/login`
