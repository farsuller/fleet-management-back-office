package org.solodev.fleet.mngt.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")

    // ── Vehicles ─────────────────────────────────────────────────
    data object Vehicles : Screen("vehicles")
    data class VehicleDetail(val vehicleId: String) : Screen("vehicles/$vehicleId")
    data object VehicleCreate : Screen("vehicles/new")

    // ── Rentals ───────────────────────────────────────────────────
    data object Rentals : Screen("rentals")
    data class RentalDetail(val rentalId: String) : Screen("rentals/$rentalId")
    data object RentalCreate : Screen("rentals/new")

    // ── Customers ─────────────────────────────────────────────────
    data object Customers : Screen("customers")
    data class CustomerDetail(val customerId: String) : Screen("customers/$customerId")
    data object CustomerCreate : Screen("customers/new")

    // ── Other modules (phases 7–10) ───────────────────────────────
    data object Maintenance : Screen("maintenance")
    data class MaintenanceDetail(val jobId: String) : Screen("maintenance/$jobId")
    data object MaintenanceCreate : Screen("maintenance/new")
    data object Accounting : Screen("accounting")
    data object LiveTracking : Screen("tracking")
    data object Reports : Screen("reports")
    data object Drivers : Screen("drivers")
    data object Users : Screen("users")
    data object VehicleStatus : Screen("vehicles/status")
    data object BookingPending : Screen("bookings/pending")
    data object CustomerFeedback : Screen("customers/feedback")
    data object SettingsPayment : Screen("settings/payment")
    data object Settings : Screen("settings")
}
