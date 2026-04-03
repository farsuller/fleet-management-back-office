# Phase 7 — Users Management (ADMIN Only)

> **Status**: `COMPLETE`
> **Prerequisite**: Phase 2
> [← Back to master plan](../web-backoffice-implementation-plan.md)

---

## 7.1 Users List (`/users`)

- [x] Table: Name · Email · Roles · Verified · Active
- [x] Route guard: ADMIN only — non-ADMIN redirected to `/dashboard`

## 7.2 User Detail (`/users/:id`)

- [x] Profile fields (read-only)
- [x] Role assignment: multi-select chip UI (`POST /v1/users/{id}/roles`)
  - Available roles: `ADMIN`, `FLEET_MANAGER`, `CUSTOMER_SUPPORT`, `RENTAL_AGENT`, `DRIVER`, `CUSTOMER`
  - Selected roles shown as dismissible chips
  - Optimistic update; reverts on API error
- [x] Delete user: two-step `ConfirmDialog` → `DELETE /v1/users/{id}`

## Caching Behavior

> `UserRepository` (to be created in this phase) · TTL **120 s**.

The `UserRepository` follows the same `InMemoryCache` pattern established in Phase 1.10:

```kotlin
class UserRepositoryImpl(private val api: FleetApiClient) : UserRepository {
    private val listCache = InMemoryCache<String, PagedResponse<UserDto>>(ttlMs = 120_000L)

    override suspend fun getUsers(cursor: String?, limit: Int, forceRefresh: Boolean): Result<...> {
        val key = "u:$cursor:$limit"
        if (!forceRefresh) listCache.get(key)?.let { return Result.success(it) }
        return api.getUsers(cursor, limit).onSuccess { listCache.put(key, it) }
    }
    // Role assignment + delete → listCache.clear()
}
```

| Action | Cache effect |
|---|---|
| Open Users list (warm) | Returns from `listCache` |
| Explicit refresh | `forceRefresh = true` |
| Assign roles / Delete user | `listCache.clear()` |
| User Detail view | Direct API call — not cached (single record) |

`UsersViewModel` should expose `isRefreshing: StateFlow<Boolean>`. Optimistic role chip updates are UI-local state only — cache is cleared after the confirmed API response, not before.

## Use Cases (Clean Architecture — Phase 1.11)

> Create these in `domain/usecase/user/UserUseCases.kt` **before** building `UsersViewModel`. See Phase 1.11 for the full checklist.

- [x] `GetUsersUseCase(userRepository)` — delegates to `userRepository.getUsers(cursor, limit, forceRefresh)`
- [x] `GetUserUseCase(userRepository)` — delegates to `userRepository.getUser(id)`
- [x] `AssignRolesUseCase(userRepository)` — delegates to `userRepository.assignRoles(userId, roles)`
- [x] `DeleteUserUseCase(userRepository)` — delegates to `userRepository.deleteUser(id)`
- [x] Register all 4 in `UseCaseModule.kt` as `factory { UseCase(get()) }`
- [x] Inject all 4 into `UsersViewModel` constructor — **never inject `UserRepository` directly**

## Verification

- [x] Non-ADMIN cannot see "Users" sidebar item
- [x] Non-ADMIN direct navigation to `/users` → redirected to `/dashboard` with snackbar
- [x] Role chip UI reverts optimistic update on API error
- [x] Delete requires explicit two-step confirmation
- [x] Deleting a user and navigating back to list shows removal (cache invalidated)
- [x] No skeleton flash on Users list revisit within 120 s
