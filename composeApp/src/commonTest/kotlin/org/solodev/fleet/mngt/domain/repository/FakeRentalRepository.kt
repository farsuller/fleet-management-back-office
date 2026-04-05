package org.solodev.fleet.mngt.domain.repository

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

    // Track interactions if needed (Alternative to Verify in MockK)
    var lastCreatedRequest: CreateRentalRequest? = null
    var wasCancelCalled = false

    override suspend fun getRentals(
        page: Int,
        limit: Int,
        status: RentalStatus?,
        forceRefresh: Boolean,
    ): Result<PagedResponse<RentalDto>> = pagedResponseResult ?: Result.failure(Exception("Not configured"))

    override suspend fun getRental(id: String): Result<RentalDto> = rentalResult ?: Result.failure(Exception("Rental not found"))

    override suspend fun createRental(request: CreateRentalRequest): Result<RentalDto> {
        lastCreatedRequest = request
        return rentalResult ?: Result.failure(Exception("Creation failed"))
    }

    override suspend fun updateRental(
        id: String,
        request: UpdateRentalRequest,
    ): Result<RentalDto> = rentalResult ?: Result.failure(Exception("Update failed"))

    override suspend fun activateRental(id: String): Result<RentalDto> = rentalResult ?: Result.failure(Exception("Activation failed"))

    override suspend fun cancelRental(id: String): Result<RentalDto> {
        wasCancelCalled = true
        return rentalResult ?: Result.success(RentalDto(id = id)) // Default success for cleanup
    }

    override suspend fun completeRental(id: String, odometer: Long): Result<RentalDto> = completeResult ?: Result.failure(Exception("Completion failed"))
}
