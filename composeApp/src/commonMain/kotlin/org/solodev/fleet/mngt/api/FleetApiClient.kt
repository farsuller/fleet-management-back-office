package org.solodev.fleet.mngt.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.solodev.fleet.mngt.api.dto.accounting.AccountDto
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.api.dto.auth.AssignRolesRequest
import org.solodev.fleet.mngt.api.dto.auth.LoginRequest
import org.solodev.fleet.mngt.api.dto.auth.LoginResponse
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.api.dto.maintenance.CompleteMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.rental.CompleteRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.tracking.FleetStatusDto
import org.solodev.fleet.mngt.api.dto.tracking.LocationHistoryEntry
import org.solodev.fleet.mngt.api.dto.tracking.RouteDto
import org.solodev.fleet.mngt.api.dto.tracking.VehicleStateDto
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.OdometerRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleDto
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleStateRequest
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.auth.TokenProvider

@Serializable
private data class ApiWrapper<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiWrapperError? = null,
)

@Serializable
private data class ApiWrapperError(
    val code: String,
    val message: String,
)

private val FleetJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

class FleetApiClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    private val authState: AuthState,
) {
    private val client = HttpClient {
        install(ContentNegotiation) { json(FleetJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        install(HttpRequestRetry) {
            maxRetries = 2
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun getUsers(cursor: String? = null, limit: Int = 20): Result<PagedResponse<UserDto>> =
        getAsPaged("/v1/users") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
        }

    suspend fun getUser(id: String): Result<UserDto> =
        get("/v1/users/$id")

    suspend fun assignRoles(id: String, request: AssignRolesRequest): Result<UserDto> =
        post("/v1/users/$id/roles", request)

    suspend fun deleteUser(id: String): Result<Unit> =
        delete("/v1/users/$id")

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun login(request: LoginRequest): Result<LoginResponse> =
        post("/v1/users/login", request)

    suspend fun logout(): Result<Unit> =
        postEmpty("/v1/auth/logout")

    suspend fun refreshToken(): Result<LoginResponse> =
        postEmpty("/v1/auth/refresh")

    // ── Vehicles ──────────────────────────────────────────────────────────────

    suspend fun getVehicles(
        cursor: String? = null,
        limit: Int = 20,
        state: String? = null,
    ): Result<PagedResponse<VehicleDto>> =
        get("/v1/vehicles") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
            state?.let { append("state", it) }
        }

    suspend fun getVehicle(id: String): Result<VehicleDto> =
        get("/v1/vehicles/$id")

    suspend fun createVehicle(request: CreateVehicleRequest): Result<VehicleDto> =
        post("/v1/vehicles", request)

    suspend fun updateVehicle(id: String, request: UpdateVehicleRequest): Result<VehicleDto> =
        put("/v1/vehicles/$id", request)

    suspend fun updateVehicleState(id: String, request: VehicleStateRequest): Result<VehicleDto> =
        patch("/v1/vehicles/$id/state", request)

    suspend fun updateOdometer(id: String, request: OdometerRequest): Result<VehicleDto> =
        patch("/v1/vehicles/$id/odometer", request)

    suspend fun deleteVehicle(id: String): Result<Unit> =
        delete("/v1/vehicles/$id")

    // ── Rentals ───────────────────────────────────────────────────────────────

    suspend fun getRentals(
        cursor: String? = null,
        limit: Int = 20,
        status: String? = null,
    ): Result<PagedResponse<RentalDto>> =
        getAsPaged("/v1/rentals") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
            status?.let { append("status", it) }
        }

    suspend fun getRental(id: String): Result<RentalDto> =
        get("/v1/rentals/$id")

    suspend fun createRental(request: CreateRentalRequest): Result<RentalDto> =
        post("/v1/rentals", request)

    suspend fun cancelRental(id: String): Result<RentalDto> =
        postEmpty("/v1/rentals/$id/cancel")

    suspend fun activateRental(id: String): Result<RentalDto> =
        postEmpty("/v1/rentals/$id/activate")

    suspend fun completeRental(id: String, request: CompleteRentalRequest): Result<RentalDto> =
        post("/v1/rentals/$id/complete", request)

    // ── Customers ─────────────────────────────────────────────────────────────

    suspend fun getCustomers(cursor: String? = null, limit: Int = 20): Result<PagedResponse<CustomerDto>> =
        getAsPaged("/v1/customers") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
        }

    suspend fun getCustomer(id: String): Result<CustomerDto> =
        get("/v1/customers/$id")

    suspend fun createCustomer(request: CreateCustomerRequest): Result<CustomerDto> =
        post("/v1/customers", request)

    suspend fun deactivateCustomer(id: String): Result<CustomerDto> = safeCall {
        client.patch("/v1/customers/$id/deactivate") {
            headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
        }.guardStatus()
    }

    // ── Maintenance ───────────────────────────────────────────────────────────

    suspend fun getMaintenanceJobs(
        cursor: String? = null,
        limit: Int = 20,
        status: String? = null,
    ): Result<PagedResponse<MaintenanceJobDto>> =
        getAsPaged("/v1/maintenance") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
            status?.let { append("status", it) }
        }

    suspend fun getMaintenanceJob(id: String): Result<MaintenanceJobDto> =
        get("/v1/maintenance/$id")

    suspend fun createMaintenanceJob(request: CreateMaintenanceRequest): Result<MaintenanceJobDto> =
        post("/v1/maintenance", request)

    suspend fun completeMaintenanceJob(id: String, request: CompleteMaintenanceRequest): Result<MaintenanceJobDto> =
        post("/v1/maintenance/$id/complete", request)

    suspend fun startMaintenanceJob(id: String): Result<MaintenanceJobDto> =
        postEmpty("/v1/maintenance/$id/start")

    suspend fun cancelMaintenanceJob(id: String): Result<MaintenanceJobDto> =
        postEmpty("/v1/maintenance/$id/cancel")

    suspend fun getMaintenanceJobsByVehicle(vehicleId: String): Result<List<MaintenanceJobDto>> =
        getList("/v1/maintenance/vehicle/$vehicleId")

    // ── Accounting ────────────────────────────────────────────────────────────

    suspend fun getInvoices(cursor: String? = null, limit: Int = 20): Result<PagedResponse<InvoiceDto>> =
        getAsPaged("/v1/accounting/invoices") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
        }

    suspend fun getInvoice(id: String): Result<InvoiceDto> =
        get("/v1/accounting/invoices/$id")

    suspend fun createInvoice(request: CreateInvoiceRequest): Result<InvoiceDto> =
        post("/v1/accounting/invoices", request)

    suspend fun payInvoice(id: String, request: PayInvoiceRequest, idempotencyKey: String): Result<PaymentDto> = safeCall {
        client.post("/v1/accounting/invoices/$id/pay") {
            headers {
                tokenProvider.token?.let { append("Authorization", "Bearer $it") }
                append("Idempotency-Key", idempotencyKey)
            }
            setBody(request)
        }.guardStatus()
    }

    suspend fun getPayments(cursor: String? = null, limit: Int = 20): Result<PagedResponse<PaymentDto>> =
        getAsPaged("/v1/accounting/payments") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
        }

    suspend fun getPaymentsByCustomer(customerId: String): Result<List<PaymentDto>> =
        getList("/v1/accounting/payments/customer/$customerId")

    suspend fun getAccounts(): Result<List<AccountDto>> =
        getList("/v1/accounting/accounts")

    suspend fun getPaymentMethods(): Result<List<PaymentMethodDto>> =
        getList("/v1/accounting/payment-methods")

    // ── Tracking ──────────────────────────────────────────────────────────────

    suspend fun getFleetStatus(): Result<FleetStatusDto> =
        getItem("/v1/tracking/status")

    suspend fun getVehicleState(vehicleId: String): Result<VehicleStateDto> =
        getItem("/v1/tracking/$vehicleId/state")

    suspend fun getLocationHistory(vehicleId: String, limit: Int = 50): Result<List<LocationHistoryEntry>> =
        get("/v1/tracking/$vehicleId/history") { append("limit", limit.toString()) }

    suspend fun getActiveRoutes(): Result<List<RouteDto>> =
        getList("/v1/tracking/routes/active")

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * For endpoints that return ApiWrapper<List<T>> instead of ApiWrapper<PagedResponse<T>>.
     * Deserializes the list and wraps it in a PagedResponse so callers stay uniform.
     */
    private suspend inline fun <reified R> getAsPaged(
        path: String,
        crossinline params: io.ktor.http.ParametersBuilder.() -> Unit = {},
    ): Result<PagedResponse<R>> = safeCall {
        val response = client.get(path) {
            headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
            url { parameters.apply(params) }
        }
        if (response.status == HttpStatusCode.Unauthorized) { authState.signOut(); throw UnauthorizedException() }
        if (response.status == HttpStatusCode.TooManyRequests) throw RateLimitException()
        if (!response.status.isSuccess()) {
            val text = runCatching { response.bodyAsText() }.getOrDefault(response.status.description)
            throw ApiException(response.status.value, text)
        }
        val wrapper = response.body<ApiWrapper<List<R>>>()
        if (!wrapper.success || wrapper.data == null) {
            val msg = wrapper.error?.message ?: "Empty response data"
            throw ApiException(response.status.value, msg)
        }
        PagedResponse(items = wrapper.data)
    }

    private suspend inline fun <reified R> get(
        path: String,
        crossinline params: io.ktor.http.ParametersBuilder.() -> Unit = {},
    ): Result<R> = safeCall {
        client.get(path) {
            headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
            url { parameters.apply(params) }
        }.guardStatus()
    }

    private suspend inline fun <reified R> getItem(path: String): Result<R> =
        get(path)

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun <reified R> getList(path: String): Result<List<R>> =
        get(path)

    private suspend inline fun <reified B : Any, reified R> post(path: String, body: B): Result<R> = safeCall {
        client.post(path) {
            headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
            setBody(body)
        }.guardStatus()
    }

    private suspend inline fun <reified R> postEmpty(path: String): Result<R> = safeCall {
        client.post(path) {
            headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
        }.guardStatus()
    }

    private suspend inline fun <reified B : Any, reified R> put(path: String, body: B): Result<R> = safeCall {
        client.put(path) {
            headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
            setBody(body)
        }.guardStatus()
    }

    private suspend inline fun <reified B : Any, reified R> patch(path: String, body: B): Result<R> = safeCall {
        client.patch(path) {
            headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
            setBody(body)
        }.guardStatus()
    }

    private suspend inline fun <reified R> delete(path: String): Result<R> = safeCall {
        client.delete(path) {
            headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
        }.guardStatus()
    }

    private suspend inline fun <reified R> HttpResponse.guardStatus(): R {
        if (status == HttpStatusCode.Unauthorized) { authState.signOut(); throw UnauthorizedException() }
        if (status == HttpStatusCode.TooManyRequests) throw RateLimitException()
        if (!status.isSuccess()) {
            val text = runCatching { bodyAsText() }.getOrDefault(status.description)
            throw ApiException(status.value, text)
        }
        @Suppress("UNCHECKED_CAST")
        if (R::class == Unit::class) return Unit as R
        val wrapper = body<ApiWrapper<R>>()
        if (!wrapper.success || wrapper.data == null) {
            val msg = wrapper.error?.message ?: "Empty response data"
            throw ApiException(status.value, msg)
        }
        return wrapper.data
    }

    private suspend inline fun <R> safeCall(block: suspend () -> R): Result<R> =
        runCatching { block() }

    fun close() = client.close()
}

class UnauthorizedException : Exception("Session expired — please log in again")
class RateLimitException : Exception("Too many requests — slow down")
class ApiException(val statusCode: Int, message: String) : Exception(message)
