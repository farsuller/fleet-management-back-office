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
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.solodev.fleet.mngt.api.dto.accounting.AccountDto
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverCollectionRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceDto
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.PaymentDto
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.api.dto.auth.AssignRoleRequest
import org.solodev.fleet.mngt.api.dto.auth.LoginRequest
import org.solodev.fleet.mngt.api.dto.auth.LoginResponse
import org.solodev.fleet.mngt.api.dto.auth.RoleDto
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest
import org.solodev.fleet.mngt.api.dto.auth.UserUpdateRequest
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
import org.solodev.fleet.mngt.api.dto.rental.RentalDto
import org.solodev.fleet.mngt.api.dto.rental.UpdateRentalRequest
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
import kotlin.reflect.typeOf

class ApiException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

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

private val FleetJson =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

class FleetApiClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    private val authState: AuthState,
) {
    private companion object {
        const val REQUEST_TIMEOUT_MS = 30_000L
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val MAX_RETRIES = 2
    }

    private val client =
        HttpClient {
            install(ContentNegotiation) { json(FleetJson) }
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
            }
            install(HttpRequestRetry) {
                maxRetries = MAX_RETRIES
                retryOnServerErrors(maxRetries = MAX_RETRIES)
                exponentialDelay()
            }
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun getUsers(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<PagedResponse<UserDto>> = getAsPaged("/v1/users") {
        cursor?.let { append("cursor", it) }
        append("limit", limit.toString())
    }

    suspend fun getUser(id: String): Result<UserDto> = get("/v1/users/$id")

    suspend fun registerUser(request: UserRegistrationRequest): Result<UserDto> = post<UserRegistrationRequest, UserDto>("/v1/users/register", request)

    suspend fun updateUser(
        id: String,
        request: UserUpdateRequest,
    ): Result<UserDto> = patch<UserUpdateRequest, UserDto>("/v1/users/$id", request)

    suspend fun assignRole(
        id: String,
        roleName: String,
    ): Result<UserDto> = post<AssignRoleRequest, UserDto>("/v1/users/$id/roles", AssignRoleRequest(roleName))

    suspend fun getRoles(): Result<List<RoleDto>> = getList("/v1/users/roles")

    suspend fun deleteUser(id: String): Result<Unit> = delete("/v1/users/$id")

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun login(request: LoginRequest): Result<LoginResponse> = post("/v1/users/login", request)

    suspend fun logout(): Result<Unit> = postEmpty("/v1/auth/logout")

    suspend fun refreshToken(): Result<LoginResponse> = postEmpty("/v1/auth/refresh")

    // ── Vehicles ──────────────────────────────────────────────────────────────

    suspend fun getVehicles(
        page: Int = 1,
        limit: Int = 20,
        state: String? = null,
    ): Result<PagedResponse<VehicleDto>> = getAsPaged("/v1/vehicles") {
        append("page", page.toString())
        append("limit", limit.toString())
        state?.let { append("state", it) }
    }

    suspend fun getVehicle(id: String): Result<VehicleDto> = get("/v1/vehicles/$id")

    suspend fun createVehicle(request: CreateVehicleRequest): Result<VehicleDto> = post("/v1/vehicles", request)

    suspend fun updateVehicle(
        id: String,
        request: UpdateVehicleRequest,
    ): Result<VehicleDto> = patch("/v1/vehicles/$id", request)

    suspend fun updateVehicleState(
        id: String,
        request: VehicleStateRequest,
    ): Result<VehicleDto> = patch("/v1/vehicles/$id/state", request)

    suspend fun updateOdometer(
        id: String,
        request: OdometerRequest,
    ): Result<VehicleDto> = post("/v1/vehicles/$id/odometer", request)

    suspend fun deleteVehicle(id: String): Result<Unit> = delete("/v1/vehicles/$id")

    suspend fun getVehicleIncidents(vehicleId: String): Result<List<VehicleIncidentDto>> = getList("/v1/vehicles/$vehicleId/incidents")

    // ── Rentals ───────────────────────────────────────────────────────────────

    suspend fun getRentals(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
    ): Result<PagedResponse<RentalDto>> = getAsPaged("/v1/rentals") {
        append("page", page.toString())
        append("limit", limit.toString())
        status?.let { append("status", it) }
    }

    suspend fun getRental(id: String): Result<RentalDto> = get("/v1/rentals/$id")

    suspend fun createRental(request: CreateRentalRequest): Result<RentalDto> = post("/v1/rentals", request)

    suspend fun cancelRental(id: String): Result<RentalDto> = postEmpty("/v1/rentals/$id/cancel")

    suspend fun activateRental(id: String): Result<RentalDto> = postEmpty("/v1/rentals/$id/activate")

    suspend fun completeRental(
        id: String,
        request: CompleteRentalRequest,
    ): Result<RentalDto> = post("/v1/rentals/$id/complete", request)

    suspend fun updateRental(
        id: String,
        request: UpdateRentalRequest,
    ): Result<RentalDto> = patch("/v1/rentals/$id", request)

    suspend fun deleteRental(id: String): Result<Unit> = delete("/v1/rentals/$id")

    // ── Customers ─────────────────────────────────────────────────────────────

    suspend fun getCustomers(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<PagedResponse<CustomerDto>> = getAsPaged("/v1/customers") {
        cursor?.let { append("cursor", it) }
        append("limit", limit.toString())
    }

    suspend fun getCustomer(id: String): Result<CustomerDto> = get("/v1/customers/$id")

    suspend fun createCustomer(request: CreateCustomerRequest): Result<CustomerDto> = post("/v1/customers", request)

    suspend fun updateCustomer(
        id: String,
        request: UpdateCustomerRequest,
    ): Result<CustomerDto> = patch("/v1/customers/$id", request)

    suspend fun deactivateCustomer(id: String): Result<CustomerDto> = safeCall {
        client
            .patch("/v1/customers/$id/deactivate") {
                headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
            }.guardStatus()
    }

    // ── Maintenance ───────────────────────────────────────────────────────────

    suspend fun getMaintenanceJobs(
        cursor: String? = null,
        limit: Int = 20,
        status: String? = null,
    ): Result<PagedResponse<MaintenanceJobDto>> = getAsPaged("/v1/maintenance/jobs") {
        cursor?.let { append("cursor", it) }
        append("limit", limit.toString())
        status?.let { append("status", it) }
    }

    suspend fun getMaintenanceJob(id: String): Result<MaintenanceJobDto> = get("/v1/maintenance/jobs/$id")

    suspend fun createMaintenanceJob(request: CreateMaintenanceRequest): Result<MaintenanceJobDto> = post("/v1/maintenance/jobs", request)

    suspend fun completeMaintenanceJob(
        id: String,
        request: CompleteMaintenanceRequest,
    ): Result<MaintenanceJobDto> = post("/v1/maintenance/jobs/$id/complete", request)

    suspend fun startMaintenanceJob(id: String): Result<MaintenanceJobDto> = postEmpty("/v1/maintenance/jobs/$id/start")

    suspend fun cancelMaintenanceJob(id: String): Result<MaintenanceJobDto> = postEmpty("/v1/maintenance/jobs/$id/cancel")

    suspend fun getMaintenanceJobsByVehicle(vehicleId: String): Result<List<MaintenanceJobDto>> = getList("/v1/maintenance/jobs/vehicle/$vehicleId")

    // ── Incidents ─────────────────────────────────────────────────────────────

    suspend fun getIncidents(
        cursor: String? = null,
        limit: Int = 20,
        status: String? = null,
    ): Result<PagedResponse<VehicleIncidentDto>> = getAsPaged("/v1/incidents") {
        cursor?.let { append("cursor", it) }
        append("limit", limit.toString())
        status?.let { append("status", it) }
    }

    // ── Accounting ────────────────────────────────────────────────────────────

    suspend fun getInvoices(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<PagedResponse<InvoiceDto>> = getAsPaged("/v1/accounting/invoices") {
        cursor?.let { append("cursor", it) }
        append("limit", limit.toString())
    }

    suspend fun getInvoice(id: String): Result<InvoiceDto> = get("/v1/accounting/invoices/$id")

    suspend fun getInvoicesByCustomer(customerId: String): Result<List<InvoiceDto>> = getList("/v1/accounting/invoices/customer/$customerId")

    suspend fun createInvoice(request: CreateInvoiceRequest): Result<InvoiceDto> = post("/v1/accounting/invoices", request)

    suspend fun payInvoice(
        id: String,
        request: PayInvoiceRequest,
        idempotencyKey: String,
    ): Result<PaymentDto> = safeCall {
        client
            .post("/v1/accounting/invoices/$id/pay") {
                headers {
                    tokenProvider.token?.let { append("Authorization", "Bearer $it") }
                    append("Idempotency-Key", idempotencyKey)
                }
                setBody(request)
            }.guardStatus()
    }

    suspend fun getPayments(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<PagedResponse<PaymentDto>> = getAsPaged("/v1/accounting/payments") {
        cursor?.let { append("cursor", it) }
        append("limit", limit.toString())
    }

    suspend fun getPaymentsByCustomer(customerId: String): Result<List<PaymentDto>> = getList("/v1/accounting/payments/customer/$customerId")

    suspend fun recordDriverCollection(request: DriverCollectionRequest): Result<PaymentDto> = post("/v1/accounting/payments/driver-collection", request)

    suspend fun getDriverPendingPayments(driverId: String): Result<List<PaymentDto>> = getList("/v1/accounting/payments/driver/$driverId/pending")

    suspend fun getAllDriverPayments(driverId: String): Result<List<PaymentDto>> = getList("/v1/accounting/payments/driver/$driverId/all")

    suspend fun submitRemittance(request: DriverRemittanceRequest): Result<DriverRemittanceDto> = post("/v1/accounting/remittances", request)

    suspend fun getRemittancesByDriver(driverId: String): Result<List<DriverRemittanceDto>> = getList("/v1/accounting/remittances/driver/$driverId")

    suspend fun getRemittance(id: String): Result<DriverRemittanceDto> = get("/v1/accounting/remittances/$id")

    suspend fun getAccounts(): Result<List<AccountDto>> = getList("/v1/accounting/accounts")

    suspend fun getPaymentMethods(): Result<List<PaymentMethodDto>> = getList("/v1/accounting/payment-methods")

    // ── Tracking ──────────────────────────────────────────────────────────────

    suspend fun getFleetStatus(): Result<FleetStatusDto> = getItem("/v1/tracking/fleet/status")

    suspend fun getVehicleState(vehicleId: String): Result<VehicleStateDto> = getItem("/v1/tracking/$vehicleId/state")

    suspend fun getLocationHistory(
        vehicleId: String,
        limit: Int = 50,
    ): Result<List<LocationHistoryEntry>> = get("/v1/tracking/$vehicleId/history") { parameters.append("limit", limit.toString()) }

    suspend fun getActiveRoutes(): Result<List<RouteDto>> = getList("/v1/tracking/routes/active")

    suspend fun createRoute(request: CreateRouteRequest): Result<RouteDto> = post("/v1/tracking/routes", request)

    // ── Coordinate Reception Control ──────────────────────────────────────────

    suspend fun getCoordinateReceptionStatus(): Result<CoordinateReceptionStatus> = getItem("/v1/tracking/admin/coordinate-reception")

    suspend fun setCoordinateReceptionEnabled(enabled: Boolean): Result<CoordinateReceptionStatus> = post("/v1/tracking/admin/coordinate-reception", CoordinateReceptionRequest(enabled))

    // ── Drivers ───────────────────────────────────────────────────────────────

    suspend fun getDrivers(): Result<List<DriverDto>> = getList("/v1/drivers")

    suspend fun getDriver(id: String): Result<DriverDto> = get("/v1/drivers/$id")

    suspend fun createDriver(request: CreateDriverRequest): Result<DriverDto> = post("/v1/drivers", request)

    suspend fun updateDriver(
        id: String,
        request: UpdateDriverRequest,
    ): Result<DriverDto> = patch("/v1/drivers/$id", request)

    suspend fun deactivateDriver(id: String): Result<DriverDto> = postEmpty("/v1/drivers/$id/deactivate")

    suspend fun activateDriver(id: String): Result<DriverDto> = postEmpty("/v1/drivers/$id/activate")

    suspend fun assignDriver(
        driverId: String,
        request: AssignDriverRequest,
    ): Result<AssignmentDto> = post("/v1/drivers/$driverId/assign", request)

    suspend fun releaseDriver(driverId: String): Result<AssignmentDto> = postEmpty("/v1/drivers/$driverId/release")

    suspend fun getDriverAssignments(driverId: String): Result<List<AssignmentDto>> = getList("/v1/drivers/$driverId/assignments")

    suspend fun getVehicleActiveDriver(vehicleId: String): Result<DriverDto> = get("/v1/vehicles/$vehicleId/driver")

    suspend fun getVehicleDriverHistory(vehicleId: String): Result<List<AssignmentDto>> = getList("/v1/vehicles/$vehicleId/driver/history")

    suspend fun startDriverShift(request: StartShiftRequest): Result<ShiftResponse> = post("/v1/drivers/shifts/start", request)

    suspend fun endDriverShift(request: EndShiftRequest): Result<ShiftResponse> = post("/v1/drivers/shifts/end", request)

    suspend fun getActiveDriverShift(): Result<ShiftResponse?> = get("/v1/drivers/shifts/active")

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private suspend inline fun <reified T> get(
        path: String,
        noinline query: (io.ktor.http.URLBuilder.() -> Unit)? = null,
    ): Result<T> = safeCall {
        client
            .get(path) {
                headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
                query?.let { url.apply(it) }
            }.guardStatus()
    }

    private suspend inline fun <reified T> getList(path: String): Result<List<T>> = get(path)

    private suspend inline fun <reified T> getItem(path: String): Result<T> = get(path)

    private suspend inline fun <reified T> getAsPaged(
        path: String,
        noinline query: (io.ktor.http.ParametersBuilder.() -> Unit)? = null,
    ): Result<PagedResponse<T>> = safeCall {
        val response =
            client
                .get(path) {
                    headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
                    query?.let { url.parameters.apply(it) }
                }

        if (!response.status.isSuccess()) {
            if (response.status == HttpStatusCode.Unauthorized) {
                authState.signOut()
            }
            val errorBody = response.bodyAsText()
            val error =
                try {
                    FleetJson.decodeFromString<ApiWrapper<Unit>>(errorBody).error
                } catch (_: Exception) {
                    null
                }
            throw ApiException(error?.message ?: "API Error: ${response.status.value} $errorBody")
        }

        val jsonElement = response.body<JsonElement>()
        val success = jsonElement.jsonObject["success"]?.jsonPrimitive?.content == "true"
        if (!success) {
            val msg =
                jsonElement.jsonObject["error"]
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonPrimitive
                    ?.content ?: "Request failed"
            throw ApiException(msg)
        }

        val data = jsonElement.jsonObject["data"]
        if (data == null || data is JsonNull) {
            throw ApiException("Empty response data")
        }

        if (data is JsonArray) {
            val items = FleetJson.decodeFromJsonElement<List<T>>(data)
            PagedResponse(items = items)
        } else {
            FleetJson.decodeFromJsonElement<PagedResponse<T>>(data)
        }
    }

    private suspend inline fun <reified R, reified T> post(
        path: String,
        body: R,
    ): Result<T> = safeCall {
        client
            .post(path) {
                headers {
                    tokenProvider.token?.let { append("Authorization", "Bearer $it") }
                }
                setBody(body)
            }.guardStatus()
    }

    private suspend inline fun <reified T> postEmpty(path: String): Result<T> = safeCall {
        client
            .post(path) {
                headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
            }.guardStatus()
    }

    private suspend inline fun <reified R, reified T> patch(
        path: String,
        body: R,
    ): Result<T> = safeCall {
        client
            .patch(path) {
                headers {
                    tokenProvider.token?.let { append("Authorization", "Bearer $it") }
                }
                setBody(body)
            }.guardStatus()
    }

    private suspend inline fun delete(path: String): Result<Unit> = safeCall {
        client
            .delete(path) {
                headers { tokenProvider.token?.let { append("Authorization", "Bearer $it") } }
            }.guardStatus()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend inline fun <reified T> HttpResponse.guardStatus(): T {
        if (!status.isSuccess()) {
            if (status == HttpStatusCode.Unauthorized) {
                authState.signOut()
            }
            val errorBody = bodyAsText()
            val error =
                try {
                    FleetJson.decodeFromString<ApiWrapper<T>>(errorBody).error
                } catch (
                    @Suppress("SwallowedException") e: SerializationException,
                ) {
                    null
                }
            throw ApiException(error?.message ?: "API Error: ${status.value} $errorBody")
        }
        val wrapper = body<ApiWrapper<T>>()
        val data = wrapper.data
        if (data != null || typeOf<T>().isMarkedNullable) {
            @Suppress("UNCHECKED_CAST")
            return data as T
        }
        throw ApiException("Empty data response")
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <T> safeCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        if (e.message?.contains("401") == true) {
            authState.signOut()
        }
        Result.failure(e)
    }
}
