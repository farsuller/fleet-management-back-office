package org.solodev.fleet.mngt.domain.repository

import kotlinx.coroutines.yield
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.RentalStatus
import org.solodev.fleet.mngt.api.dto.rental.UpdateRentalRequest
import org.solodev.fleet.mngt.repository.RentalRepository

class FakeRentalRepository : RentalRepository {
    // Properties to control the behavior of the fake during tests
    var rentalResult: Result<RentalDto>? = null
    var completeResult: Result<RentalDto>? = null
    var pagedResponseResult: Result<PagedResponse<RentalDto>>? = null
    var deleteResult: Result<Unit> = Result.success(Unit)

    // Track interactions if needed (Alternative to Verify in MockK)
    var lastPage: Int? = null
    var lastLimit: Int? = null
    var lastStatus: RentalStatus? = null
    var lastForceRefresh: Boolean? = null
    var lastRequestedRentalId: String? = null
    var lastCreatedRequest: CreateRentalRequest? = null
    var lastUpdatedRentalId: String? = null
    var lastUpdatedRequest: UpdateRentalRequest? = null
    var lastActivatedRentalId: String? = null
    var lastCancelledRentalId: String? = null
    var lastDeletedRentalId: String? = null
    var lastCompletedRentalId: String? = null
    var lastCompletedOdometerKm: Long? = null
    var wasCancelCalled = false
    var suspendOnGetRental = false
    var suspendOnUpdateRental = false
    var suspendOnActivateRental = false
    var suspendOnCompleteRental = false

    override suspend fun getRentals(
        page: Int,
        limit: Int,
        status: RentalStatus?,
        forceRefresh: Boolean,
    ): Result<PagedResponse<RentalDto>> {
        lastPage = page
        lastLimit = limit
        lastStatus = status
        lastForceRefresh = forceRefresh
        return pagedResponseResult ?: Result.failure(Exception("Not configured"))
    }

    override suspend fun getRental(id: String): Result<RentalDto> {
        lastRequestedRentalId = id
        if (suspendOnGetRental) {
            yield()
        }
        return rentalResult ?: Result.failure(Exception("Rental not found"))
    }

    override suspend fun createRental(request: CreateRentalRequest): Result<RentalDto> {
        lastCreatedRequest = request
        return rentalResult ?: Result.failure(Exception("Creation failed"))
    }

    override suspend fun updateRental(
        id: String,
        request: UpdateRentalRequest,
    ): Result<RentalDto> {
        lastUpdatedRentalId = id
        lastUpdatedRequest = request
        if (suspendOnUpdateRental) {
            yield()
        }
        return rentalResult ?: Result.failure(Exception("Update failed"))
    }

    override suspend fun activateRental(id: String): Result<RentalDto> {
        lastActivatedRentalId = id
        if (suspendOnActivateRental) {
            yield()
        }
        return rentalResult ?: Result.failure(Exception("Activation failed"))
    }

    override suspend fun cancelRental(id: String): Result<RentalDto> {
        lastCancelledRentalId = id
        wasCancelCalled = true
        return rentalResult ?: Result.success(RentalDto(id = id)) // Default success for cleanup
    }

    override suspend fun deleteRental(id: String): Result<Unit> {
        lastDeletedRentalId = id
        return deleteResult
    }

    override suspend fun completeRental(
        id: String,
        finalOdometerKm: Long,
    ): Result<RentalDto> {
        lastCompletedRentalId = id
        lastCompletedOdometerKm = finalOdometerKm
        if (suspendOnCompleteRental) {
            yield()
        }
        return completeResult ?: Result.failure(Exception("Completion failed"))
    }
}
