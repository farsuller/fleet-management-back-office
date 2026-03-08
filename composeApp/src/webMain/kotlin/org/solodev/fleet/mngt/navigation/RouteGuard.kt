package org.solodev.fleet.mngt.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.features.auth.LoginScreen
import org.solodev.fleet.mngt.features.accounting.AccountingScreen
import org.solodev.fleet.mngt.features.maintenance.MaintenanceDetailScreen
import org.solodev.fleet.mngt.features.maintenance.MaintenanceListScreen
import org.solodev.fleet.mngt.features.customers.CustomerDetailScreen
import org.solodev.fleet.mngt.features.customers.CustomersListScreen
import org.solodev.fleet.mngt.features.dashboard.DashboardScreen
import org.solodev.fleet.mngt.features.rentals.RentalDetailScreen
import org.solodev.fleet.mngt.features.rentals.RentalsListScreen
import org.solodev.fleet.mngt.features.settings.SettingsScreen
import org.solodev.fleet.mngt.features.users.UserDetailScreen
import org.solodev.fleet.mngt.features.users.UsersListScreen
import org.solodev.fleet.mngt.features.vehicles.VehicleDetailScreen
import org.solodev.fleet.mngt.features.vehicles.VehiclesListScreen
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun RouteGuard() {
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val router = remember { AppRouter() }
    val authStatus by dispatcher.status.collectAsState()

    LaunchedEffect(authStatus) {
        when (authStatus) {
            is AuthStatus.Authenticated -> if (router.currentScreen is Screen.Login) {
                router.clearAndNavigate(Screen.Dashboard)
            }
            is AuthStatus.Unauthenticated -> router.clearAndNavigate(Screen.Login)
            AuthStatus.Loading -> Unit
        }
    }

    when (authStatus) {
        AuthStatus.Loading -> LoadingScreen()
        else -> AppNavHost(router = router)
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(fleetColors.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = fleetColors.primary)
    }
}

@Composable
fun AppNavHost(router: AppRouter) {
    val screen = router.currentScreen
    when (screen) {
        is Screen.Login      -> LoginScreen(router = router)
        is Screen.Dashboard  -> AppShell(router = router) { DashboardScreen(router = router) }

        is Screen.Vehicles       -> AppShell(router = router) { VehiclesListScreen(router = router) }
        is Screen.VehicleDetail  -> AppShell(router = router) { VehicleDetailScreen(screen.vehicleId, router = router) }
        is Screen.VehicleCreate  -> AppShell(router = router) { VehiclesListScreen(router = router) }

        is Screen.Rentals        -> AppShell(router = router) { RentalsListScreen(router = router) }
        is Screen.RentalDetail   -> AppShell(router = router) { RentalDetailScreen(screen.rentalId, router = router) }
        is Screen.RentalCreate   -> AppShell(router = router) { RentalsListScreen(router = router) }

        is Screen.Customers      -> AppShell(router = router) { CustomersListScreen(router = router) }
        is Screen.CustomerDetail -> AppShell(router = router) { CustomerDetailScreen(screen.customerId, router = router) }
        is Screen.CustomerCreate -> AppShell(router = router) { CustomersListScreen(router = router) }

        is Screen.Users       -> AppShell(router = router) { UsersListScreen(router = router) }
        is Screen.UserDetail -> AppShell(router = router) { UserDetailScreen(screen.userId, router = router) }

        is Screen.Accounting -> AppShell(router = router) { AccountingScreen(router = router) }

        is Screen.Maintenance       -> AppShell(router = router) { MaintenanceListScreen(router = router) }
        is Screen.MaintenanceDetail -> AppShell(router = router) { MaintenanceDetailScreen(screen.jobId, router = router) }
        is Screen.MaintenanceCreate -> AppShell(router = router) { MaintenanceListScreen(router = router) }

        is Screen.Settings    -> AppShell(router = router) { SettingsScreen() }

        else -> AppShell(router = router) {
            // Remaining feature screens will be wired in future phases
            Box(Modifier.fillMaxSize())
        }
    }
}
