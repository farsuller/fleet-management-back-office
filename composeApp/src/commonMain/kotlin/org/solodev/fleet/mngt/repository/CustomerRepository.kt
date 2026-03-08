package org.solodev.fleet.mngt.repository

import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.api.PagedResponse
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.cache.InMemoryCache

interface CustomerRepository {
    suspend fun getCustomers(cursor: String? = null, limit: Int = 20, forceRefresh: Boolean = false): Result<PagedResponse<CustomerDto>>
    suspend fun getCustomer(id: String): Result<CustomerDto>
    suspend fun createCustomer(request: CreateCustomerRequest): Result<CustomerDto>
    suspend fun deactivateCustomer(id: String): Result<CustomerDto>
}

class CustomerRepositoryImpl(private val api: FleetApiClient) : CustomerRepository {

    // 120-second TTL — customer records change infrequently within a session
    private val listCache = InMemoryCache<String, PagedResponse<CustomerDto>>(ttlMs = 120_000L)

    override suspend fun getCustomers(
        cursor: String?,
        limit: Int,
        forceRefresh: Boolean,
    ): Result<PagedResponse<CustomerDto>> {
        val key = "c:$cursor:$limit"
        if (!forceRefresh) listCache.get(key)?.let { return Result.success(it) }
        return api.getCustomers(cursor, limit).onSuccess { listCache.put(key, it) }
    }

    override suspend fun getCustomer(id: String) = api.getCustomer(id)

    override suspend fun createCustomer(request: CreateCustomerRequest) =
        api.createCustomer(request).onSuccess { listCache.clear() }

    override suspend fun deactivateCustomer(id: String) =
        api.deactivateCustomer(id).onSuccess { listCache.clear() }
}
