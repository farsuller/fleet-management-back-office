package org.solodev.fleet.mngt.repository

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.InMemoryTokenProvider
import org.solodev.fleet.mngt.auth.SecureStorage
import org.solodev.fleet.mngt.auth.UserSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryImplJvmTest {
    @Test
    fun shouldCoverAuthRepositoryImpl_LoginLogoutAndRehydrate() = runTest {
        TestApiServer().use { server ->
            server.json("POST", "/v1/users/login", success("""{"token":"token-1","user":{"id":"user-1","email":"user@example.com","firstName":"Jane","lastName":"Doe","roles":["ADMIN"]}}"""))
            server.json("POST", "/v1/auth/logout", success("{}"))
            server.json("POST", "/v1/auth/refresh", success("""{"token":"token-2","user":{"id":"user-1","email":"user@example.com","firstName":"Jane","lastName":"Doe","roles":["ADMIN"]}}"""))

            val tokenProvider = InMemoryTokenProvider()
            val repository = AuthRepositoryImpl(api(server, tokenProvider, FakeAuthState()), AppDependencyDispatcher(tokenProvider, SecureStorage()))

            val login = repository.login("user@example.com", "secret")
            val logout = repository.logout()
            val rehydrate = repository.rehydrate()

            assertTrue(login.isSuccess)
            assertTrue(logout.isSuccess)
            assertTrue(rehydrate.isSuccess)
            assertEquals("token-2", tokenProvider.token)
        }
    }

    @Test
    fun shouldCoverUserRepositoryImpl_CacheAndMutations() = runTest {
        TestApiServer().use { server ->
            server.json("GET", "/v1/users", success("""{"items":[{"id":"user-1","email":"user@example.com"}],"nextCursor":"next","total":1}"""))
            server.json("GET", "/v1/users/roles", success("""[{"id":"role-1","name":"ADMIN"}]"""))
            server.json("POST", "/v1/users/register", success("""{"id":"user-2","email":"new@example.com"}"""))
            server.json("POST", "/v1/users/user-1/roles", success("""{"id":"user-1","roles":["ADMIN"]}"""))
            server.json("DELETE", "/v1/users/user-1", success("{}"))

            val repository = UserRepositoryImpl(api(server))

            assertTrue(repository.getUsers().isSuccess)
            assertTrue(repository.getUsers().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/users"))

            assertTrue(repository.getRoles().isSuccess)
            assertTrue(repository.getRoles().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/users/roles"))

            assertTrue(repository.registerUser(org.solodev.fleet.mngt.api.dto.auth.UserRegistrationRequest("new@example.com", "secret", "New", "User")).isSuccess)
            assertTrue(repository.getUsers().isSuccess)
            assertEquals(2, server.callCount("GET", "/v1/users"))

            assertTrue(repository.assignRole("user-1", "ADMIN").isSuccess)
            assertTrue(repository.deleteUser("user-1").isSuccess)
        }
    }

    @Test
    fun shouldCoverCustomerRepositoryImpl_CacheAndMutations() = runTest {
        TestApiServer().use { server ->
            server.json("GET", "/v1/customers", success("""{"items":[{"id":"customer-1","email":"customer@example.com"}],"total":1}"""))
            server.json("PATCH", "/v1/customers/customer-1", success("""{"id":"customer-1","phone":"999"}"""))
            server.json("PATCH", "/v1/customers/customer-1/deactivate", success("""{"id":"customer-1","isActive":false}"""))

            val repository = CustomerRepositoryImpl(api(server))

            assertTrue(repository.getCustomers().isSuccess)
            assertTrue(repository.getCustomers().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/customers"))

            assertTrue(repository.updateCustomer("customer-1", org.solodev.fleet.mngt.api.dto.customer.UpdateCustomerRequest(phone = "999")).isSuccess)
            assertTrue(repository.getCustomers().isSuccess)
            assertEquals(2, server.callCount("GET", "/v1/customers"))

            assertTrue(repository.deactivateCustomer("customer-1").isSuccess)
        }
    }

    @Test
    fun shouldCoverVehicleRepositoryImpl_CacheAndMutations() = runTest {
        TestApiServer().use { server ->
            server.json("GET", "/v1/vehicles", success("""{"items":[{"id":"vehicle-1","state":"AVAILABLE"}],"total":1}"""))
            server.json("PATCH", "/v1/vehicles/vehicle-1/state", success("""{"id":"vehicle-1","state":"MAINTENANCE"}"""))
            server.json("POST", "/v1/vehicles/vehicle-1/odometer", success("""{"id":"vehicle-1","mileageKm":1200}"""))

            val repository = VehicleRepositoryImpl(api(server))

            assertTrue(repository.getVehicles().isSuccess)
            assertTrue(repository.getVehicles().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/vehicles"))

            assertTrue(repository.updateVehicleState("vehicle-1", org.solodev.fleet.mngt.api.dto.vehicle.VehicleState.MAINTENANCE).isSuccess)
            assertTrue(repository.getVehicles().isSuccess)
            assertEquals(2, server.callCount("GET", "/v1/vehicles"))

            assertTrue(repository.updateOdometer("vehicle-1", 1200L).isSuccess)
        }
    }

    @Test
    fun shouldCoverDriverRepositoryImpl_CacheAndMutations() = runTest {
        TestApiServer().use { server ->
            server.json("GET", "/v1/drivers", success("""[{"id":"driver-1","email":"driver@example.com"}]"""))
            server.json("POST", "/v1/drivers/driver-1/assign", success("""{"id":"assignment-1","driverId":"driver-1","vehicleId":"vehicle-1"}"""))
            server.json("POST", "/v1/drivers/driver-1/release", success("""{"id":"assignment-1","driverId":"driver-1","isActive":false}"""))

            val repository = DriverRepositoryImpl(api(server))

            assertTrue(repository.getDrivers().isSuccess)
            assertTrue(repository.getDrivers().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/drivers"))

            assertTrue(repository.assignToVehicle("driver-1", org.solodev.fleet.mngt.api.dto.driver.AssignDriverRequest("vehicle-1")).isSuccess)
            assertTrue(repository.getDrivers().isSuccess)
            assertEquals(2, server.callCount("GET", "/v1/drivers"))

            assertTrue(repository.releaseFromVehicle("driver-1").isSuccess)
        }
    }

    @Test
    fun shouldCoverRentalRepositoryImpl_CacheAndMutations() = runTest {
        TestApiServer().use { server ->
            server.json("GET", "/v1/rentals", success("""{"items":[{"id":"rental-1","vehicleId":"vehicle-1"}],"total":1}"""))
            server.json("POST", "/v1/rentals/rental-1/complete", success("""{"id":"rental-1","vehicleId":"vehicle-1"}"""))
            server.json("DELETE", "/v1/rentals/rental-1", success("{}"))

            val repository = RentalRepositoryImpl(api(server))

            assertTrue(repository.getRentals().isSuccess)
            assertTrue(repository.getRentals().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/rentals"))

            assertTrue(repository.completeRental("rental-1", 5000L).isSuccess)
            assertTrue(repository.getRentals().isSuccess)
            assertEquals(2, server.callCount("GET", "/v1/rentals"))

            assertTrue(repository.deleteRental("rental-1").isSuccess)
        }
    }

    @Test
    fun shouldCoverMaintenanceRepositoryImpl_CacheAndMutations() = runTest {
        TestApiServer().use { server ->
            server.json("GET", "/v1/maintenance/jobs", success("""{"items":[{"id":"job-1","vehicleId":"vehicle-1"}],"total":1}"""))
            server.json("POST", "/v1/maintenance/jobs/job-1/complete", success("""{"id":"job-1","vehicleId":"vehicle-1"}"""))
            server.json("GET", "/v1/incidents", success("""{"items":[],"total":0}"""))

            val repository = MaintenanceRepositoryImpl(api(server))

            assertTrue(repository.getJobs().isSuccess)
            assertTrue(repository.getJobs().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/maintenance/jobs"))

            assertTrue(repository.completeJob("job-1", 100L, 200L).isSuccess)
            assertTrue(repository.getJobs().isSuccess)
            assertEquals(2, server.callCount("GET", "/v1/maintenance/jobs"))

            assertTrue(repository.getIncidents().isSuccess)
        }
    }

    @Test
    fun shouldCoverAccountingRepositoryImpl_CachesAndInvalidates() = runTest {
        TestApiServer().use { server ->
            server.json("GET", "/v1/accounting/invoices", success("""{"items":[{"id":"invoice-1","total":1000}],"total":1}"""))
            server.json("GET", "/v1/accounting/payments", success("""{"items":[{"id":"payment-1","amount":500}],"total":1}"""))
            server.json("POST", "/v1/accounting/invoices/invoice-1/pay", success("""{"id":"payment-2","invoiceId":"invoice-1","amount":500}"""))

            val repository = AccountingRepositoryImpl(api(server))

            assertTrue(repository.getInvoices().isSuccess)
            assertTrue(repository.getInvoices().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/accounting/invoices"))

            assertTrue(repository.getPayments().isSuccess)
            assertTrue(repository.getPayments().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/accounting/payments"))

            assertTrue(repository.payInvoice("invoice-1", org.solodev.fleet.mngt.api.dto.accounting.PayInvoiceRequest(amount = 500L, paymentMethod = "CARD"), "idem-1").isSuccess)
            assertTrue(repository.getInvoices().isSuccess)
            assertTrue(repository.getPayments().isSuccess)
            assertEquals(2, server.callCount("GET", "/v1/accounting/invoices"))
            assertEquals(2, server.callCount("GET", "/v1/accounting/payments"))
        }
    }

    @Test
    fun shouldCoverTrackingRepositoryImpl_CachesAndInvalidates() = runTest {
        TestApiServer().use { server ->
            server.json("GET", "/v1/tracking/fleet/status", success("""{"totalVehicles":5,"activeVehicles":3}"""))
            server.json("GET", "/v1/tracking/routes/active", success("""[{"id":"route-1","name":"North"}]"""))
            server.json("POST", "/v1/tracking/routes", success("""{"id":"route-2","name":"South"}"""))
            server.json("GET", "/v1/tracking/vehicle-1/history", success("""[{"latitude":14.6,"longitude":121.0}]"""))

            val repository = TrackingRepositoryImpl(api(server))

            assertTrue(repository.getFleetStatus().isSuccess)
            assertTrue(repository.getFleetStatus().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/tracking/fleet/status"))

            assertTrue(repository.getActiveRoutes().isSuccess)
            assertTrue(repository.getActiveRoutes().isSuccess)
            assertEquals(1, server.callCount("GET", "/v1/tracking/routes/active"))

            assertTrue(repository.createRoute("South", null, "{}" ).isSuccess)
            assertTrue(repository.getActiveRoutes().isSuccess)
            assertEquals(2, server.callCount("GET", "/v1/tracking/routes/active"))

            assertTrue(repository.getLocationHistory("vehicle-1").isSuccess)
        }
    }

    private fun api(
        server: TestApiServer,
        tokenProvider: InMemoryTokenProvider = InMemoryTokenProvider(),
        authState: AuthState = FakeAuthState(),
    ) = FleetApiClient(server.baseUrl, tokenProvider, authState)

    private fun success(data: String) = "{\"success\":true,\"data\":$data}"
}

private class FakeAuthState : AuthState {
    override val status = MutableStateFlow<AuthStatus>(AuthStatus.Unauthenticated)

    override fun signIn(
        token: String,
        session: UserSession,
    ) {
        status.value = AuthStatus.Authenticated(session)
    }

    override fun signOut() {
        status.value = AuthStatus.Unauthenticated
    }
}

private class TestApiServer : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val routes = linkedMapOf<String, (HttpExchange) -> Unit>()
    private val callCounts = linkedMapOf<String, Int>()

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange ->
            val key = "${exchange.requestMethod} ${exchange.requestURI.path}"
            callCounts[key] = (callCounts[key] ?: 0) + 1
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

    fun callCount(
        method: String,
        path: String,
    ): Int = callCounts["$method $path"] ?: 0

    override fun close() {
        server.stop(0)
    }
}