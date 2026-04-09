package org.solodev.fleet.mngt.api

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverCollectionRequest
import org.solodev.fleet.mngt.api.dto.accounting.DriverRemittanceRequest
import org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest
import org.solodev.fleet.mngt.api.dto.auth.LoginRequest
import org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest
import org.solodev.fleet.mngt.api.dto.auth.UserUpdateRequest
import org.solodev.fleet.mngt.api.dto.customer.CreateCustomerRequest
import org.solodev.fleet.mngt.api.dto.customer.UpdateCustomerRequest
import org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.CreateDriverRequest
import org.solodev.fleet.mngt.api.dto.driver.EndShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.StartShiftRequest
import org.solodev.fleet.mngt.api.dto.driver.UpdateDriverRequest
import org.solodev.fleet.mngt.api.dto.maintenance.CompleteMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.CreateMaintenanceRequest
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenancePriority
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceType
import org.solodev.fleet.mngt.api.dto.rental.CompleteRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.CreateRentalRequest
import org.solodev.fleet.mngt.api.dto.rental.UpdateRentalRequest
import org.solodev.fleet.mngt.api.dto.vehicle.CreateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.OdometerRequest
import org.solodev.fleet.mngt.api.dto.vehicle.UpdateVehicleRequest
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleState
import org.solodev.fleet.mngt.api.dto.vehicle.VehicleStateRequest
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.InMemoryTokenProvider
import org.solodev.fleet.mngt.auth.UserSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class FleetApiClientJvmTest {
    @Test
    fun shouldHandleReadEndpointsAndDefaultArguments() = runTest {
        FleetClientTestServer().use { server ->
            server.json("GET", "/v1/users", success("""{"items":[{"id":"user-1","email":"user@example.com"}],"nextCursor":"next","total":1}"""))
            server.json("GET", "/v1/users/user-1", success("""{"id":"user-1","email":"user@example.com"}"""))
            server.json("GET", "/v1/users/roles", success("""[{"id":"role-1","name":"ADMIN"}]"""))
            server.json("GET", "/v1/vehicles", success("""{"items":[{"id":"vehicle-1","state":"AVAILABLE"}],"total":1}"""))
            server.json("GET", "/v1/vehicles/vehicle-1", success("""{"id":"vehicle-1","state":"AVAILABLE"}"""))
            server.json("GET", "/v1/vehicles/vehicle-1/incidents", success("""[{"id":"incident-1","vehicleId":"vehicle-1","title":"Scratch","description":"Minor scratch","severity":"LOW","status":"REPORTED"}]"""))
            server.json("GET", "/v1/rentals", success("""{"items":[{"id":"rental-1","vehicleId":"vehicle-1"}],"total":1}"""))
            server.json("GET", "/v1/rentals/rental-1", success("""{"id":"rental-1","vehicleId":"vehicle-1"}"""))
            server.json("GET", "/v1/customers", success("""{"items":[{"id":"customer-1","email":"customer@example.com"}],"total":1}"""))
            server.json("GET", "/v1/customers/customer-1", success("""{"id":"customer-1","email":"customer@example.com"}"""))
            server.json("GET", "/v1/maintenance/jobs", success("""{"items":[{"id":"job-1","vehicleId":"vehicle-1"}],"total":1}"""))
            server.json("GET", "/v1/maintenance/jobs/job-1", success("""{"id":"job-1","vehicleId":"vehicle-1"}"""))
            server.json("GET", "/v1/maintenance/jobs/vehicle/vehicle-1", success("""[{"id":"job-1","vehicleId":"vehicle-1"}]"""))
            server.json("GET", "/v1/incidents", success("""{"items":[{"id":"incident-2","vehicleId":"vehicle-1","title":"Dent","description":"Door dent","severity":"LOW","status":"REPORTED"}],"total":1}"""))
            server.json("GET", "/v1/accounting/invoices", success("""{"items":[{"id":"invoice-1","total":1000}],"total":1}"""))
            server.json("GET", "/v1/accounting/invoices/invoice-1", success("""{"id":"invoice-1","total":1000}"""))
            server.json("GET", "/v1/accounting/invoices/customer/customer-1", success("""[{"id":"invoice-1","total":1000}]"""))
            server.json("GET", "/v1/accounting/payments", success("""{"items":[{"id":"payment-1","amount":500}],"total":1}"""))
            server.json("GET", "/v1/accounting/payments/customer/customer-1", success("""[{"id":"payment-1","amount":500}]"""))
            server.json("GET", "/v1/accounting/payments/driver/driver-1/pending", success("""[{"id":"payment-2","amount":600}]"""))
            server.json("GET", "/v1/accounting/payments/driver/driver-1/all", success("""[{"id":"payment-3","amount":700}]"""))
            server.json("GET", "/v1/accounting/remittances/driver/driver-1", success("""[{"id":"remittance-1","driverId":"driver-1"}]"""))
            server.json("GET", "/v1/accounting/remittances/remittance-1", success("""{"id":"remittance-1","driverId":"driver-1"}"""))
            server.json("GET", "/v1/accounting/accounts", success("""[{"id":"account-1","accountCode":"1000","accountType":"ASSET","balance":1000}]"""))
            server.json("GET", "/v1/accounting/payment-methods", success("""[{"id":"pm-1","name":"Cash"}]"""))
            server.json("GET", "/v1/tracking/fleet/status", success("""{"totalVehicles":3,"activeVehicles":2}"""))
            server.json("GET", "/v1/tracking/vehicle-1/state", success("""{"vehicleId":"vehicle-1","latitude":14.6,"longitude":121.0}"""))
            server.json("GET", "/v1/tracking/vehicle-1/history", success("""[{"latitude":14.6,"longitude":121.0}]"""))
            server.json("GET", "/v1/tracking/routes/active", success("""[{"id":"route-1","name":"North"}]"""))
            server.json("GET", "/v1/tracking/admin/coordinate-reception", success("""{"enabled":true,"updatedBy":"admin"}"""))
            server.json("GET", "/v1/drivers", success("""[{"id":"driver-1","email":"driver@example.com"}]"""))
            server.json("GET", "/v1/drivers/driver-1", success("""{"id":"driver-1","email":"driver@example.com"}"""))
            server.json("GET", "/v1/drivers/driver-1/assignments", success("""[{"id":"assignment-1","driverId":"driver-1","vehicleId":"vehicle-1"}]"""))
            server.json("GET", "/v1/vehicles/vehicle-1/driver", success("""{"id":"driver-1","email":"driver@example.com"}"""))
            server.json("GET", "/v1/vehicles/vehicle-1/driver/history", success("""[{"id":"assignment-2","driverId":"driver-1","vehicleId":"vehicle-1"}]"""))
            server.json("GET", "/v1/drivers/shifts/active", success("""{"id":"shift-1","driverId":"driver-1","vehicleId":"vehicle-1","isActive":true}"""))

            val client = api(server)

            assertTrue(client.getUsers().isSuccess)
            assertTrue(client.getUser("user-1").isSuccess)
            assertTrue(client.getRoles().isSuccess)
            assertTrue(client.getVehicles().isSuccess)
            assertTrue(client.getVehicle("vehicle-1").isSuccess)
            assertTrue(client.getVehicleIncidents("vehicle-1").isSuccess)
            assertTrue(client.getRentals().isSuccess)
            assertTrue(client.getRental("rental-1").isSuccess)
            assertTrue(client.getCustomers().isSuccess)
            assertTrue(client.getCustomer("customer-1").isSuccess)
            assertTrue(client.getMaintenanceJobs().isSuccess)
            assertTrue(client.getMaintenanceJob("job-1").isSuccess)
            assertTrue(client.getMaintenanceJobsByVehicle("vehicle-1").isSuccess)
            assertTrue(client.getIncidents().isSuccess)
            assertTrue(client.getInvoices().isSuccess)
            assertTrue(client.getInvoice("invoice-1").isSuccess)
            assertTrue(client.getInvoicesByCustomer("customer-1").isSuccess)
            assertTrue(client.getPayments().isSuccess)
            assertTrue(client.getPaymentsByCustomer("customer-1").isSuccess)
            assertTrue(client.getDriverPendingPayments("driver-1").isSuccess)
            assertTrue(client.getAllDriverPayments("driver-1").isSuccess)
            assertTrue(client.getRemittancesByDriver("driver-1").isSuccess)
            assertTrue(client.getRemittance("remittance-1").isSuccess)
            assertTrue(client.getAccounts().isSuccess)
            assertTrue(client.getPaymentMethods().isSuccess)
            assertTrue(client.getFleetStatus().isSuccess)
            assertTrue(client.getVehicleState("vehicle-1").isSuccess)
            assertTrue(client.getLocationHistory("vehicle-1").isSuccess)
            assertTrue(client.getActiveRoutes().isSuccess)
            assertTrue(client.getCoordinateReceptionStatus().isSuccess)
            assertTrue(client.getDrivers().isSuccess)
            assertTrue(client.getDriver("driver-1").isSuccess)
            assertTrue(client.getDriverAssignments("driver-1").isSuccess)
            assertTrue(client.getVehicleActiveDriver("vehicle-1").isSuccess)
            assertTrue(client.getVehicleDriverHistory("vehicle-1").isSuccess)
            assertTrue(client.getActiveDriverShift().isSuccess)
        }
    }

    @Test
    fun shouldHandleMutationEndpoints() = runTest {
        FleetClientTestServer().use { server ->
            server.json("POST", "/v1/users/register", success("""{"id":"user-2","email":"new@example.com"}"""))
            server.json("PATCH", "/v1/users/user-1", success("""{"id":"user-1","firstName":"Updated"}"""))
            server.json("POST", "/v1/users/user-1/roles", success("""{"id":"user-1","roles":["ADMIN"]}"""))
            server.json("DELETE", "/v1/users/user-1", success("{}"))
            server.json("POST", "/v1/users/login", success("""{"token":"token-1","user":{"id":"user-1","email":"user@example.com"}}"""))
            server.json("POST", "/v1/auth/logout", success("{}"))
            server.json("POST", "/v1/auth/refresh", success("""{"token":"token-2","user":{"id":"user-1","email":"user@example.com"}}"""))
            server.json("POST", "/v1/vehicles", success("""{"id":"vehicle-2","vin":"VIN-1"}"""))
            server.json("PATCH", "/v1/vehicles/vehicle-1", success("""{"id":"vehicle-1","color":"Blue"}"""))
            server.json("PATCH", "/v1/vehicles/vehicle-1/state", success("""{"id":"vehicle-1","state":"MAINTENANCE"}"""))
            server.json("POST", "/v1/vehicles/vehicle-1/odometer", success("""{"id":"vehicle-1","mileageKm":1500}"""))
            server.json("DELETE", "/v1/vehicles/vehicle-1", success("{}"))
            server.json("POST", "/v1/rentals", success("""{"id":"rental-2","vehicleId":"vehicle-1"}"""))
            server.json("POST", "/v1/rentals/rental-1/cancel", success("""{"id":"rental-1","status":"CANCELLED"}"""))
            server.json("POST", "/v1/rentals/rental-1/activate", success("""{"id":"rental-1","status":"ACTIVE"}"""))
            server.json("POST", "/v1/rentals/rental-1/complete", success("""{"id":"rental-1","status":"COMPLETED"}"""))
            server.json("PATCH", "/v1/rentals/rental-1", success("""{"id":"rental-1","vehicleId":"vehicle-1"}"""))
            server.json("DELETE", "/v1/rentals/rental-1", success("{}"))
            server.json("POST", "/v1/customers", success("""{"id":"customer-2","email":"newcustomer@example.com"}"""))
            server.json("PATCH", "/v1/customers/customer-1", success("""{"id":"customer-1","phone":"999"}"""))
            server.json("PATCH", "/v1/customers/customer-1/deactivate", success("""{"id":"customer-1","isActive":false}"""))
            server.json("POST", "/v1/maintenance/jobs", success("""{"id":"job-2","vehicleId":"vehicle-1"}"""))
            server.json("POST", "/v1/maintenance/jobs/job-1/complete", success("""{"id":"job-1","status":"COMPLETED"}"""))
            server.json("POST", "/v1/maintenance/jobs/job-1/start", success("""{"id":"job-1","status":"IN_PROGRESS"}"""))
            server.json("POST", "/v1/maintenance/jobs/job-1/cancel", success("""{"id":"job-1","status":"CANCELLED"}"""))
            server.json("POST", "/v1/accounting/invoices", success("""{"id":"invoice-2","total":1000}"""))
            server.json("POST", "/v1/accounting/invoices/invoice-1/pay", success("""{"id":"payment-5","invoiceId":"invoice-1","amount":500}"""))
            server.json("POST", "/v1/accounting/payments/driver-collection", success("""{"id":"payment-6","amount":500}"""))
            server.json("POST", "/v1/accounting/remittances", success("""{"id":"remittance-2","driverId":"driver-1"}"""))
            server.json("POST", "/v1/tracking/routes", success("""{"id":"route-2","name":"South"}"""))
            server.json("POST", "/v1/tracking/admin/coordinate-reception", success("""{"enabled":false,"updatedBy":"admin"}"""))
            server.json("POST", "/v1/drivers", success("""{"id":"driver-2","email":"newdriver@example.com"}"""))
            server.json("PATCH", "/v1/drivers/driver-1", success("""{"id":"driver-1","phone":"999999"}"""))
            server.json("POST", "/v1/drivers/driver-1/deactivate", success("""{"id":"driver-1","isActive":false}"""))
            server.json("POST", "/v1/drivers/driver-1/activate", success("""{"id":"driver-1","isActive":true}"""))
            server.json("POST", "/v1/drivers/driver-1/assign", success("""{"id":"assignment-3","driverId":"driver-1","vehicleId":"vehicle-1"}"""))
            server.json("POST", "/v1/drivers/driver-1/release", success("""{"id":"assignment-3","driverId":"driver-1","isActive":false}"""))
            server.json("POST", "/v1/drivers/shifts/start", success("""{"id":"shift-2","driverId":"driver-1","vehicleId":"vehicle-1","isActive":true}"""))
            server.json("POST", "/v1/drivers/shifts/end", success("""{"id":"shift-2","driverId":"driver-1","vehicleId":"vehicle-1","isActive":false}"""))

            val client = api(server)

            assertTrue(client.registerUser(UserRegistrationRequest("new@example.com", "secret", "New", "User")).isSuccess)
            assertTrue(client.updateUser("user-1", UserUpdateRequest(firstName = "Updated")).isSuccess)
            assertTrue(client.assignRole("user-1", "ADMIN").isSuccess)
            assertTrue(client.deleteUser("user-1").isSuccess)
            assertTrue(client.login(LoginRequest("user@example.com", "secret")).isSuccess)
            assertTrue(client.logout().isSuccess)
            assertTrue(client.refreshToken().isSuccess)
            assertTrue(client.createVehicle(CreateVehicleRequest("VIN-1", "ABC123", "Toyota", "Vios", 2024, "White")).isSuccess)
            assertTrue(client.updateVehicle("vehicle-1", UpdateVehicleRequest(color = "Blue")).isSuccess)
            assertTrue(client.updateVehicleState("vehicle-1", VehicleStateRequest(VehicleState.MAINTENANCE)).isSuccess)
            assertTrue(client.updateOdometer("vehicle-1", OdometerRequest(1500L)).isSuccess)
            assertTrue(client.deleteVehicle("vehicle-1").isSuccess)
            assertTrue(client.createRental(CreateRentalRequest("customer-1", "vehicle-1", "2030-01-01T00:00:00Z", "2030-01-02T00:00:00Z", 1500L)).isSuccess)
            assertTrue(client.cancelRental("rental-1").isSuccess)
            assertTrue(client.activateRental("rental-1").isSuccess)
            assertTrue(client.completeRental("rental-1", CompleteRentalRequest(5000L)).isSuccess)
            assertTrue(client.updateRental("rental-1", UpdateRentalRequest(endDate = "2030-01-03T00:00:00Z")).isSuccess)
            assertTrue(client.deleteRental("rental-1").isSuccess)
            assertTrue(client.createCustomer(CreateCustomerRequest("newcustomer@example.com", "New", "Customer", "123456789", "ABC-123", "2030-01-01")).isSuccess)
            assertTrue(client.updateCustomer("customer-1", UpdateCustomerRequest(phone = "999")).isSuccess)
            assertTrue(client.deactivateCustomer("customer-1").isSuccess)
            assertTrue(client.createMaintenanceJob(CreateMaintenanceRequest("vehicle-1", MaintenanceType.PREVENTIVE, MaintenancePriority.HIGH, 1_700_000_000_000, 5000L, "Oil change")).isSuccess)
            assertTrue(client.completeMaintenanceJob("job-1", CompleteMaintenanceRequest(100L, 200L)).isSuccess)
            assertTrue(client.startMaintenanceJob("job-1").isSuccess)
            assertTrue(client.cancelMaintenanceJob("job-1").isSuccess)
            assertTrue(client.createInvoice(CreateInvoiceRequest("customer-1", subtotal = 1000L, dueDate = "2030-01-10")).isSuccess)
            assertTrue(client.payInvoice("invoice-1", PayInvoiceRequest(500L, "CARD"), "idem-1").isSuccess)
            assertTrue(client.recordDriverCollection(DriverCollectionRequest("driver-1", "customer-1", "invoice-1", 500L, "CASH")).isSuccess)
            assertTrue(client.submitRemittance(DriverRemittanceRequest("driver-1", listOf("payment-1"))).isSuccess)
            assertTrue(client.createRoute(org.solodev.fleet.mngt.api.dto.tracking.CreateRouteRequest("South", null, "{}" )).isSuccess)
            assertTrue(client.setCoordinateReceptionEnabled(false).isSuccess)
            assertTrue(client.createDriver(CreateDriverRequest("newdriver@example.com", "New", "Driver", "1234567890", "LIC-123", "2030-01-01")).isSuccess)
            assertTrue(client.updateDriver("driver-1", UpdateDriverRequest(phone = "999999")).isSuccess)
            assertTrue(client.deactivateDriver("driver-1").isSuccess)
            assertTrue(client.activateDriver("driver-1").isSuccess)
            assertTrue(client.assignDriver("driver-1", AssignDriverRequest("vehicle-1")).isSuccess)
            assertTrue(client.releaseDriver("driver-1").isSuccess)
            assertTrue(client.startDriverShift(StartShiftRequest("vehicle-1")).isSuccess)
            assertTrue(client.endDriverShift(EndShiftRequest()).isSuccess)
        }
    }

    @Test
    fun shouldSignOutOnUnauthorizedAndSurfaceWrapperErrors() = runTest {
        FleetClientTestServer().use { server ->
            server.json("GET", "/v1/users/user-401", "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Unauthorized\"}}", status = 401)
            server.json("GET", "/v1/vehicles", "{\"success\":false,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"Vehicles failed\"}}")

            val authState = RecordingAuthState()
            val client = api(server, authState = authState)

            val unauthorized = client.getUser("user-401")
            val wrapperFailure = client.getVehicles()

            assertTrue(unauthorized.isFailure)
            assertTrue(wrapperFailure.isFailure)
            assertTrue(authState.signOutCount > 0)
            assertEquals("Vehicles failed", wrapperFailure.exceptionOrNull()?.message)
        }
    }

    @Test
    fun shouldSurfaceEmptyDataAndWrapperFailures_ForHelperBranches() = runTest {
        FleetClientTestServer().use { server ->
            server.json("GET", "/v1/users", "{\"success\":true,\"data\":null}")
            server.json("GET", "/v1/accounting/invoices", "{\"success\":false,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"Invoice listing failed\"}}")
            server.json("POST", "/v1/drivers", "{\"success\":false,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"Create driver failed\"}}", status = 400)
            server.json("PATCH", "/v1/drivers/driver-1", "{\"success\":false,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"Update driver failed\"}}", status = 400)
            server.json("DELETE", "/v1/vehicles/vehicle-1", "{\"success\":false,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"Delete vehicle failed\"}}", status = 400)

            val client = api(server)

            val emptyUsers = client.getUsers()
            val invoicesFailure = client.getInvoices()
            val createDriverFailure = client.createDriver(CreateDriverRequest("newdriver@example.com", "New", "Driver", "1234567890", "LIC-123", "2030-01-01"))
            val updateDriverFailure = client.updateDriver("driver-1", UpdateDriverRequest(phone = "999999"))
            val deleteVehicleFailure = client.deleteVehicle("vehicle-1")

            assertTrue(emptyUsers.isFailure)
            assertEquals("Empty response data", emptyUsers.exceptionOrNull()?.message)
            assertTrue(invoicesFailure.isFailure)
            assertEquals("Invoice listing failed", invoicesFailure.exceptionOrNull()?.message)
            assertTrue(createDriverFailure.isFailure)
            assertEquals("Create driver failed", createDriverFailure.exceptionOrNull()?.message)
            assertTrue(updateDriverFailure.isFailure)
            assertEquals("Update driver failed", updateDriverFailure.exceptionOrNull()?.message)
            assertTrue(deleteVehicleFailure.isFailure)
            assertEquals("Delete vehicle failed", deleteVehicleFailure.exceptionOrNull()?.message)
        }
    }

    @Test
    fun shouldHandleNullActiveShiftResult() = runTest {
        FleetClientTestServer().use { server ->
            server.json("GET", "/v1/drivers/shifts/active", success("null"))

            val client = api(server)
            val result = client.getActiveDriverShift()

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }

    private fun api(
        server: FleetClientTestServer,
        authState: RecordingAuthState = RecordingAuthState(),
    ): FleetApiClient {
        val tokenProvider = InMemoryTokenProvider().apply { setToken("test-token") }
        return FleetApiClient(server.baseUrl, tokenProvider, authState)
    }

    private fun success(data: String) = "{\"success\":true,\"data\":$data}"
}

private class RecordingAuthState : AuthState {
    override val status = MutableStateFlow<AuthStatus>(AuthStatus.Unauthenticated)
    var signOutCount = 0

    override fun signIn(
        token: String,
        session: UserSession,
    ) {
        status.value = AuthStatus.Authenticated(session)
    }

    override fun signOut() {
        signOutCount += 1
        status.value = AuthStatus.Unauthenticated
    }
}

private class FleetClientTestServer : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val routes = linkedMapOf<String, (HttpExchange) -> Unit>()

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange ->
            val key = "${exchange.requestMethod} ${exchange.requestURI.path}"
            val handler = routes[key]
            if (handler == null) {
                exchange.sendResponseHeaders(404, 0)
                exchange.responseBody.use { it.write("not found".toByteArray()) }
            } else {
                handler(exchange)
            }
        }
        server.start()
    }

    fun json(
        method: String,
        path: String,
        body: String,
        status: Int = 200,
    ) {
        routes["$method $path"] = { exchange ->
            val bytes = body.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    override fun close() {
        server.stop(0)
    }
}