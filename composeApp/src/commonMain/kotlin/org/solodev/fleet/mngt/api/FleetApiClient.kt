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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.solodev.fleet.mngt.api.dto.accounting.AccountDto
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverCollectionRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceDto
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceRequest
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
import org.solodev.fleet.mngt.api.dto.customer.UpdateCustomerRequest
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.AssignmentDto
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.DriverDto
import org.solodev.fleet.mngt.api.dto.driver.EndShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.ShiftResponse
import org.solodev.fleet.mngt.api.dto.driver.StartShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.UpdateDriverRequest
import org.solodev.fleet.mngt.api.dto.maintenance.CompleteMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.VehicleIncidentDto
import org.solodev.fleet.mngt.api.dto.rental.CompleteRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.UpdateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.tracking.CoordinateReceptionRequest
import org.solodev.fleet.mngt.api.dto.tracking.CoordinateReceptionStatus
import org.solodev.fleet.mngt.api.dto.tracking.CreateRouteRequest
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
    val metadata: Map<String, JsonElement>? = null,
    val requestId: String? = null,
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
        page: Int = 1,
        limit: Int = 20,
        state: String? = null,
    ): Result<PagedResponse<VehicleDto>> =
        getAsPaged("/v1/vehicles") {
            append("page", page.toString())
            append("limit", limit.toString())
            state?.let { append("state", it) }
        }

    suspend fun getVehicle(id: String): Result<VehicleDto> =
        get("/v1/vehicles/$id")

    suspend fun createVehicle(request: CreateVehicleRequest): Result<VehicleDto> =
        post("/v1/vehicles", request)

    suspend fun updateVehicle(id: String, request: UpdateVehicleRequest): Result<VehicleDto> =
        patch("/v1/vehicles/$id", request)

    suspend fun updateVehicleState(id: String, request: VehicleStateRequest): Result<VehicleDto> =
        patch("/v1/vehicles/$id/state", request)

    suspend fun updateOdometer(id: String, request: OdometerRequest): Result<VehicleDto> =
        post("/v1/vehicles/$id/odometer", request)

    suspend fun deleteVehicle(id: String): Result<Unit> =
        delete("/v1/vehicles/$id")

    suspend fun getVehicleIncidents(vehicleId: String): Result<List<VehicleIncidentDto>> =
        getList("/v1/vehicles/$vehicleId/incidents")

    // ── Rentals ───────────────────────────────────────────────────────────────

    suspend fun getRentals(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
    ): Result<PagedResponse<RentalDto>> =
        getAsPaged("/v1/rentals") {
            append("page", page.toString())
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

    suspend fun updateRental(id: String, request: UpdateRentalRequest): Result<RentalDto> =
        patch("/v1/rentals/$id", request)

    suspend fun deleteRental(id: String): Result<Unit> =
        delete("/v1/rentals/$id")

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

    suspend fun updateCustomer(id: String, request: UpdateCustomerRequest): Result<CustomerDto> =
        patch("/v1/customers/$id", request)

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
        getAsPaged("/v1/maintenance/jobs") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
            status?.let { append("status", it) }
        }

    suspend fun getMaintenanceJob(id: String): Result<MaintenanceJobDto> =
        get("/v1/maintenance/jobs/$id")

    suspend fun createMaintenanceJob(request: CreateMaintenanceRequest): Result<MaintenanceJobDto> =
        post("/v1/maintenance/jobs", request)

    suspend fun completeMaintenanceJob(id: String, request: CompleteMaintenanceRequest): Result<MaintenanceJobDto> =
        post("/v1/maintenance/jobs/$id/complete", request)

    suspend fun startMaintenanceJob(id: String): Result<MaintenanceJobDto> =
        postEmpty("/v1/maintenance/jobs/$id/start")

    suspend fun cancelMaintenanceJob(id: String): Result<MaintenanceJobDto> =
        postEmpty("/v1/maintenance/jobs/$id/cancel")

    suspend fun getMaintenanceJobsByVehicle(vehicleId: String): Result<List<MaintenanceJobDto>> =
        getList("/v1/maintenance/jobs/vehicle/$vehicleId")

    // ── Incidents ─────────────────────────────────────────────────────────────

    suspend fun getIncidents(
        cursor: String? = null,
        limit: Int = 20,
        status: String? = null,
    ): Result<PagedResponse<VehicleIncidentDto>> =
        getAsPaged("/v1/incidents") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
            status?.let { append("status", it) }
        }

    // ── Accounting ────────────────────────────────────────────────────────────

    suspend fun getInvoices(cursor: String? = null, limit: Int = 20): Result<PagedResponse<InvoiceDto>> =
        getAsPaged("/v1/accounting/invoices") {
            cursor?.let { append("cursor", it) }
            append("limit", limit.toString())
        }

    suspend fun getInvoice(id: String): Result<InvoiceDto> =
        get("/v1/accounting/invoices/$id")

    suspend fun getInvoicesByCustomer(customerId: String): Result<List<InvoiceDto>> =
        getList("/v1/accounting/invoices/customer/$customerId")

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

    suspend fun recordDriverCollection(request: DriverCollectionRequest): Result<PaymentDto> =
        post("/v1/accounting/payments/driver-collection", request)

    suspend fun getDriverPendingPayments(driverId: String): Result<List<PaymentDto>> =
        getList("/v1/accounting/payments/driver/$driverId/pending")

    suspend fun getAllDriverPayments(driverId: String): Result<List<PaymentDto>> =
        getList("/v1/accounting/payments/driver/$driverId/all")

    suspend fun submitRemittance(request: DriverRemittanceRequest): Result<DriverRemittanceDto> =
        post("/v1/accounting/remittances", request)

    suspend fun getRemittancesByDriver(driverId: String): Result<List<DriverRemittanceDto>> =
        getList("/v1/accounting/remittances/driver/$driverId")

    suspend fun getRemittance(id: String): Result<DriverRemittanceDto> =
        get("/v1/accounting/remittances/$id")

    suspend fun getAccounts(): Result<List<AccountDto>> =
        getList("/v1/accounting/accounts")

    suspend fun getPaymentMethods(): Result<List<PaymentMethodDto>> =
        getList("/v1/accounting/payment-methods")

    // ── Tracking ──────────────────────────────────────────────────────────────

    suspend fun getFleetStatus(): Result<FleetStatusDto> =
        getItem("/v1/tracking/fleet/status")

    suspend fun getVehicleState(vehicleId: String): Result<VehicleStateDto> =
        getItem("/v1/tracking/$vehicleId/state")

    suspend fun getLocationHistory(vehicleId: String, limit: Int = 50): Result<List<LocationHistoryEntry>> =
        get("/v1/tracking/$vehicleId/history") { append("limit", limit.toString()) }

    suspend fun getActiveRoutes(): Result<List<RouteDto>> =
        getList("/v1/tracking/routes/active")

    suspend fun createRoute(request: CreateRouteRequest): Result<RouteDto> =
        post("/v1/tracking/routes", request)

    // ── Coordinate Reception Control ──────────────────────────────────────────

    suspend fun getCoordinateReceptionStatus(): Result<CoordinateReceptionStatus> =
        getItem("/v1/tracking/admin/coordinate-reception")

    suspend fun setCoordinateReceptionEnabled(enabled: Boolean): Result<CoordinateReceptionStatus> =
        post("/v1/tracking/admin/coordinate-reception", CoordinateReceptionRequest(enabled))

    // ── Drivers ───────────────────────────────────────────────────────────────

    suspend fun getDrivers(): Result<List<DriverDto>> =
        getList("/v1/drivers")

    suspend fun getDriver(id: String): Result<DriverDto> =
        get("/v1/drivers/$id")

    suspend fun createDriver(request: CreateDriverRequest): Result<DriverDto> =
        post("/v1/drivers", request)

    suspend fun deactivateDriver(id: String): Result<DriverDto> =
        postEmpty("/v1/drivers/$id/deactivate")

    suspend fun activateDriver(id: String): Result<DriverDto> =
        postEmpty("/v1/drivers/$id/activate")

    suspend fun updateDriver(id: String, request: UpdateDriverRequest): Result<DriverDto> =
        patch("/v1/drivers/$id", request)

    suspend fun assignDriver(driverId: String, request: AssignDriverRequest): Result<AssignmentDto> =
        post("/v1/drivers/$driverId/assign", request)

    suspend fun releaseDriver(driverId: String): Result<AssignmentDto> =
        postEmpty("/v1/drivers/$driverId/release")

    suspend fun getDriverAssignments(driverId: String): Result<List<AssignmentDto>> =
        getList("/v1/drivers/$driverId/assignments")

    suspend fun getVehicleActiveDriver(vehicleId: String): Result<DriverDto> =
        get("/v1/vehicles/$vehicleId/driver")

    suspend fun getVehicleDriverHistory(vehicleId: String): Result<List<AssignmentDto>> =
        getList("/v1/vehicles/$vehicleId/driver/history")

    // ── Driver Shifts ────────────────────────────────────────────────────────

    suspend fun startDriverShift(request: StartShiftRequest): Result<ShiftResponse> =
        post("/v1/drivers/shifts/start", request)

    suspend fun endDriverShift(request: EndShiftRequest): Result<ShiftResponse> =
        post("/v1/drivers/shifts/end", request)

    suspend fun getActiveDriverShift(): Result<ShiftResponse?> =
        getItem("/v1/drivers/shifts/active")

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
        val responseText = response.bodyAsText()
        val jsonElement = FleetJson.decodeFromString<JsonElement>(responseText)
        val success = jsonElement.jsonObject["success"]?.jsonPrimitive?.content == "true"
        
        if (!success) {
            val errorElement = jsonElement.jsonObject["error"]
            val msg = if (errorElement != null) {
                FleetJson.decodeFromJsonElement<ApiWrapperError>(errorElement).message
            } else "Request failed"
            throw ApiException(response.status.value, msg)
        }

        val data = jsonElement.jsonObject["data"] ?: throw ApiException(response.status.value, "Empty response data")
        
        if (data is JsonArray) {
            val items = FleetJson.decodeFromJsonElement<List<R>>(data)
            PagedResponse(items = items)
        } else {
            FleetJson.decodeFromJsonElement<PagedResponse<R>>(data)
        }
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
            val raw = runCatching { bodyAsText() }.getOrDefault(status.description)
            val message = runCatching {
                FleetJson.decodeFromString<ApiWrapper<Unit>>(raw).error?.message
            }.getOrNull() ?: raw
            throw ApiException(status.value, message)
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
