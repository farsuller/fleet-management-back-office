package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.rental.CompleteRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.api.dto.rental.UpdateRentalRequest
import org.solodev.fleet.mngt.cache.InMemoryCache

interface RentalRepository {
    suspend fun getRentals(
        page: Int = 1,
        limit: Int = 20,
        status: RentalStatus? = null,
        forceRefresh: Boolean = false,
    ): Result<PagedResponse<RentalDto>>

    suspend fun getRental(id: String): Result<RentalDto>

    suspend fun createRental(request: CreateRentalRequest): Result<RentalDto>

    suspend fun updateRental(
        id: String,
        request: UpdateRentalRequest,
    ): Result<RentalDto>

    suspend fun activateRental(id: String): Result<RentalDto>

    suspend fun cancelRental(id: String): Result<RentalDto>

    suspend fun deleteRental(id: String): Result<Unit>

    suspend fun completeRental(
        id: String,
        finalOdometerKm: Long,
    ): Result<RentalDto>
}

class RentalRepositoryImpl(
    private val api: FleetApiClient,
) : RentalRepository {
    // 30-second TTL — rental status changes frequently (activate, cancel, complete)
    private val listCache = InMemoryCache<String, PagedResponse<RentalDto>>(ttlMs = 30_000L)

    override suspend fun getRentals(
        page: Int,
        limit: Int,
        status: RentalStatus?,
        forceRefresh: Boolean,
    ): Result<PagedResponse<RentalDto>> {
        val key = "r:$page:$limit:${status?.name}"
        if (!forceRefresh) listCache.get(key)?.let { return Result.success(it) }
        return api.getRentals(page, limit, status?.name).onSuccess { listCache.put(key, it) }
    }

    override suspend fun getRental(id: String) = api.getRental(id)

    override suspend fun createRental(request: CreateRentalRequest) = api.createRental(request).onSuccess { listCache.clear() }

    override suspend fun updateRental(
        id: String,
        request: UpdateRentalRequest,
    ) = api.updateRental(id, request).onSuccess { listCache.clear() }

    override suspend fun activateRental(id: String) = api.activateRental(id).onSuccess { listCache.clear() }

    override suspend fun cancelRental(id: String) = api.cancelRental(id).onSuccess { listCache.clear() }

    override suspend fun deleteRental(id: String) = api.deleteRental(id).onSuccess { listCache.clear() }

    override suspend fun completeRental(
        id: String,
        finalOdometerKm: Long,
    ) = api
        .completeRental(id, CompleteRentalRequest(finalOdometerKm))
        .onSuccess { listCache.clear() }
}
