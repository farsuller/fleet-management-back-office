package org.solodev.fleet.mngt.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.solodev.fleet.mngt.domain.usecase.accounting.CreateInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetAccountsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetAllDriverPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetDriverPendingPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoicesUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentMethodsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetRemittancesByDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.RecordDriverCollectionUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.SubmitRemittanceUseCase
import org.solodev.fleet.mngt.domain.usecase.auth.LoginUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.CreateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.DeactivateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomersUseCase
import org.solodev.fleet.mngt.domain.usecase.dashboard.GetDashboardUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.ActivateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CancelRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CompleteRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CreateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetPaymentMethodsUseCase as RentalGetPaymentMethodsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.PayInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.user.AssignRolesUseCase
import org.solodev.fleet.mngt.domain.usecase.user.DeleteUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetUsersUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.CreateVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.DeleteVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleLocationHistoryUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehiclesUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateOdometerUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateVehicleStateUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateVehicleUseCase
import org.solodev.fleet.mngt.features.accounting.AccountsViewModel
import org.solodev.fleet.mngt.features.accounting.DriverPaymentsViewModel
import org.solodev.fleet.mngt.features.accounting.InvoicesViewModel
import org.solodev.fleet.mngt.features.accounting.PaymentsViewModel
import org.solodev.fleet.mngt.features.accounting.RemittancesViewModel
import org.solodev.fleet.mngt.features.auth.LoginViewModel
import org.solodev.fleet.mngt.features.customers.CustomersViewModel
import org.solodev.fleet.mngt.features.dashboard.DashboardViewModel
import org.solodev.fleet.mngt.features.rentals.RentalsViewModel
import org.solodev.fleet.mngt.domain.usecase.maintenance.CancelMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.CompleteMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.GetMaintenanceJobUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.GetMaintenanceJobsUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.ScheduleMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.StartMaintenanceUseCase
import org.solodev.fleet.mngt.features.maintenance.MaintenanceViewModel
import org.solodev.fleet.mngt.domain.usecase.driver.AssignDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.CreateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.DeactivateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetDriversUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.ReleaseDriverUseCase
import org.solodev.fleet.mngt.features.drivers.DriversViewModel
import org.solodev.fleet.mngt.features.tracking.FleetTrackingViewModel
import org.solodev.fleet.mngt.features.users.UsersViewModel
import org.solodev.fleet.mngt.features.vehicles.VehiclesViewModel

val viewModelModule = module {
    factory { LoginViewModel(loginUseCase = get()) }
    factory { DashboardViewModel(getDashboardUseCase = get()) }
    factory {
        VehiclesViewModel(
            getVehiclesUseCase = get(),
            getVehicleUseCase = get(),
            createVehicleUseCase = get(),
            updateVehicleUseCase = get(),
            updateVehicleStateUseCase = get(),
            updateOdometerUseCase = get(),
            deleteVehicleUseCase = get(),
            getVehicleMaintenanceUseCase = get(),
            getVehicleLocationHistoryUseCase = get(),
        )
    }
    factory {
        RentalsViewModel(
            getRentalsUseCase = get(),
            getRentalUseCase = get(),
            createRentalUseCase = get(),
            activateRentalUseCase = get(),
            cancelRentalUseCase = get(),
            completeRentalUseCase = get(),
            getPaymentMethodsUseCase = get(named("rentalPaymentMethods")),
            payInvoiceUseCase = get(),
        )
    }
    factory {
        CustomersViewModel(
            getCustomersUseCase = get(),
            getCustomerUseCase = get(),
            createCustomerUseCase = get(),
            deactivateCustomerUseCase = get(),
            getCustomerRentalsUseCase = get(),
            getCustomerPaymentsUseCase = get(),
        )
    }
    factory {
        InvoicesViewModel(
            getInvoicesUseCase = get(),
            getInvoiceUseCase = get(),
            getPaymentMethodsUseCase = get<GetPaymentMethodsUseCase>(),
            payInvoiceUseCase = get(),
            createInvoiceUseCase = get(),
        )
    }
    factory { PaymentsViewModel(getPaymentsUseCase = get()) }
    factory { AccountsViewModel(getAccountsUseCase = get()) }
    factory {
        DriverPaymentsViewModel(
            getDriversUseCase = get(),
            getDriverPendingPaymentsUseCase = get(),
            getAllDriverPaymentsUseCase = get(),
            recordDriverCollectionUseCase = get(),
        )
    }
    factory {
        RemittancesViewModel(
            getDriversUseCase = get(),
            getDriverPendingPaymentsUseCase = get(),
            getRemittancesByDriverUseCase = get(),
            submitRemittanceUseCase = get(),
        )
    }
    factory {
        MaintenanceViewModel(
            getMaintenanceJobsUseCase = get(),
            getMaintenanceJobUseCase = get(),
            scheduleMaintenanceUseCase = get(),
            startMaintenanceUseCase = get(),
            completeMaintenanceUseCase = get(),
            cancelMaintenanceUseCase = get(),
            getVehiclesUseCase = get(),
        )
    }
    factory {
        UsersViewModel(
            getUsersUseCase = get(),
            getUserUseCase = get(),
            assignRolesUseCase = get(),
            deleteUserUseCase = get(),
        )
    }
    factory {
        FleetTrackingViewModel(
            getActiveRoutesUseCase = get(),
            getFleetStatusUseCase = get(),
            fleetLiveClient = get(),
            trackingRepository = get(),
        )
    }
    factory {
        DriversViewModel(
            getDriversUseCase = get(),
            createDriverUseCase = get(),
            deactivateDriverUseCase = get(),
            assignDriverUseCase = get(),
            releaseDriverUseCase = get(),
            driverRepository = get(),
        )
    }
}
