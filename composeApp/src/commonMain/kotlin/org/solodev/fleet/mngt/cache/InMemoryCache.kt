package org.solodev.fleet.mngt.cache

import kotlin.time.Clock

/**
 * Simple in-memory TTL cache for Kotlin/JS (browser) targets where Room/SQLite is unavailable.
 *
 * Designed for a stale-while-revalidate pattern:
 * - [get] returns data only if within TTL — used on first-load paths
 * - [getStale] returns data regardless of TTL — reserved for future SWR composable helpers
 * - Repository list-fetching methods call [get]; on miss they hit the network and call [put]
 * - Write operations (create/update/delete) call [clear] or [invalidate] to keep cache coherent
 * - Explicit user refresh bypasses the cache via the `forceRefresh` parameter propagated from
 *   the ViewModel down through repository calls
 *
 * TTL defaults:
 *   Vehicles      — 120 s (low mutation rate)
 *   Rentals       —  30 s (high mutation rate)
 *   Maintenance   — 120 s
 *   Invoices/Payments — 60 s
 *
 * @param ttlMs Time-to-live in milliseconds.
 */
class InMemoryCache<K : Any, V : Any>(private val ttlMs: Long = 60_000L) {

    private data class Entry<V>(val value: V, val cachedAt: Long)

    private val store = LinkedHashMap<K, Entry<V>>()

    /** Returns a fresh (within TTL) entry, or null if expired / absent. */
    fun get(key: K): V? {
        val entry = store[key] ?: return null
        return if (age(entry) <= ttlMs) entry.value else null
    }

    /**
     * Returns the most recent cached value regardless of TTL.
     * Useful for stale-while-revalidate: emit old data to suppress skeletons,
     * then overwrite once the network response arrives.
     */
    fun getStale(key: K): V? = store[key]?.value

    /** Returns true when an entry is present but has outlived its TTL. */
    fun isExpired(key: K): Boolean {
        val entry = store[key] ?: return true
        return age(entry) > ttlMs
    }

    fun put(key: K, value: V) {
        store[key] = Entry(value, Clock.System.now().toEpochMilliseconds())
    }

    fun invalidate(key: K) { store.remove(key) }

    fun clear() { store.clear() }

    private fun age(entry: Entry<V>): Long =
        Clock.System.now().toEpochMilliseconds() - entry.cachedAt
}
