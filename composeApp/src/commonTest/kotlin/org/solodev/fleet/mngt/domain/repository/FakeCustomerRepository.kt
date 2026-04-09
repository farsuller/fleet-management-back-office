package org.solodev.fleet.mngt.domain.repository

import kotlinx.coroutines.yield
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.customer.UpdateCustomerRequest
import org.solodev.fleet.mngt.repository.CustomerRepository

class FakeCustomerRepository : CustomerRepository {
    var customersResult: Result<PagedResponse<CustomerDto>>? = null
    var customerResult: Result<CustomerDto>? = null

    var lastCursor: String? = null
    var lastLimit: Int? = null
    var lastForceRefresh: Boolean? = null
    var lastCustomerId: String? = null
    var lastCreateRequest: CreateCustomerRequest? = null
    var lastUpdateRequest: UpdateCustomerRequest? = null
    var suspendOnUpdateCustomer = false

    override suspend fun getCustomers(
        cursor: String?,
        limit: Int,
        forceRefresh: Boolean,
    ): Result<PagedResponse<CustomerDto>> {
        lastCursor = cursor
        lastLimit = limit
        lastForceRefresh = forceRefresh
        return customersResult ?: Result.failure(Exception("Customers not configured"))
    }

    override suspend fun getCustomer(id: String): Result<CustomerDto> {
        lastCustomerId = id
        return customerResult ?: Result.failure(Exception("Customer not found"))
    }

    override suspend fun createCustomer(request: CreateCustomerRequest): Result<CustomerDto> {
        lastCreateRequest = request
        return customerResult ?: Result.failure(Exception("Customer creation not configured"))
    }

    override suspend fun updateCustomer(
        id: String,
        request: UpdateCustomerRequest,
    ): Result<CustomerDto> {
        lastCustomerId = id
        lastUpdateRequest = request
        if (suspendOnUpdateCustomer) {
            yield()
        }
        return customerResult ?: Result.failure(Exception("Customer update not configured"))
    }

    override suspend fun deactivateCustomer(id: String): Result<CustomerDto> {
        lastCustomerId = id
        return customerResult ?: Result.failure(Exception("Customer deactivation not configured"))
    }
}