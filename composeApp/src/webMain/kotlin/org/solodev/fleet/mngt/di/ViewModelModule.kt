package org.solodev.fleet.mngt.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentMethodsUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.CreateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomersUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.ActivateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CancelRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CompleteRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CreateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.DeleteRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.PayInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.UpdateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehiclesUseCase
import org.solodev.fleet.mngt.features.accounting.AccountsViewModel
import org.solodev.fleet.mngt.features.accounting.DriverPaymentsViewModel
import org.solodev.fleet.mngt.features.accounting.InvoicesViewModel
import org.solodev.fleet.mngt.features.accounting.PaymentsViewModel
import org.solodev.fleet.mngt.features.accounting.RemittancesViewModel
import org.solodev.fleet.mngt.features.auth.LoginViewModel
import org.solodev.fleet.mngt.features.customers.CustomersViewModel
import org.solodev.fleet.mngt.features.dashboard.DashboardViewModel
import org.solodev.fleet.mngt.features.drivers.DriversViewModel
import org.solodev.fleet.mngt.features.maintenance.MaintenanceViewModel
import org.solodev.fleet.mngt.features.rentals.RentalsViewModel
import org.solodev.fleet.mngt.features.tracking.FleetTrackingViewModel
import org.solodev.fleet.mngt.features.users.UsersViewModel
import org.solodev.fleet.mngt.features.vehicles.VehiclesViewModel
import org.solodev.fleet.mngt.domain.usecase.rental.GetPaymentMethodsUseCase as RentalGetPaymentMethodsUseCase

val viewModelModule = module {
    viewModel { LoginViewModel(loginUseCase = get()) }
    viewModel { DashboardViewModel(getDashboardUseCase = get(), authState = get()) }
    viewModel {
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
            getVehicleIncidentsUseCase = get(),
            authState = get(),
        )
    }

    viewModel {
        RentalsViewModel(
            getRentalsUseCase = get<GetRentalsUseCase>(),
            getRentalUseCase = get<GetRentalUseCase>(),
            createRentalUseCase = get<CreateRentalUseCase>(),
            updateRentalUseCase = get<UpdateRentalUseCase>(),
            activateRentalUseCase = get<ActivateRentalUseCase>(),
            cancelRentalUseCase = get<CancelRentalUseCase>(),
            completeRentalUseCase = get<CompleteRentalUseCase>(),
            getPaymentMethodsUseCase = get<RentalGetPaymentMethodsUseCase>(org.koin.core.qualifier.named("rentalPaymentMethods")),
            payInvoiceUseCase = get<PayInvoiceUseCase>(),
            getVehiclesUseCase = get<GetVehiclesUseCase>(),
            getVehicleUseCase = get<GetVehicleUseCase>(),
            getCustomersUseCase = get<GetCustomersUseCase>(),
            getCustomerUseCase = get<GetCustomerUseCase>(),
            createCustomerUseCase = get<CreateCustomerUseCase>(),
            deleteRentalUseCase = get<DeleteRentalUseCase>(),
            authState = get<AuthState>(),
        )
    }
    viewModel {
        CustomersViewModel(
            getCustomersUseCase = get(),
            getCustomerUseCase = get(),
            createCustomerUseCase = get(),
            updateCustomerUseCase = get(),
            deactivateCustomerUseCase = get(),
            getCustomerRentalsUseCase = get(),
            getCustomerPaymentsUseCase = get(),
            authState = get(),
        )
    }
    viewModel {
        InvoicesViewModel(
            getInvoicesUseCase = get(),
            getInvoiceUseCase = get(),
            getPaymentMethodsUseCase = get<GetPaymentMethodsUseCase>(),
            payInvoiceUseCase = get(),
            createInvoiceUseCase = get(),
        )
    }
    viewModel { PaymentsViewModel(getPaymentsUseCase = get()) }
    viewModel { AccountsViewModel(getAccountsUseCase = get()) }
    viewModel {
        DriverPaymentsViewModel(
            getDriversUseCase = get(),
            getDriverPendingPaymentsUseCase = get(),
            getAllDriverPaymentsUseCase = get(),
            recordDriverCollectionUseCase = get(),
        )
    }
    viewModel {
        RemittancesViewModel(
            getDriversUseCase = get(),
            getDriverPendingPaymentsUseCase = get(),
            getRemittancesByDriverUseCase = get(),
            submitRemittanceUseCase = get(),
        )
    }
    viewModel {
        MaintenanceViewModel(
            getMaintenanceJobsUseCase = get(),
            getMaintenanceJobUseCase = get(),
            scheduleMaintenanceUseCase = get(),
            startMaintenanceUseCase = get(),
            completeMaintenanceUseCase = get(),
            cancelMaintenanceUseCase = get(),
            getVehiclesUseCase = get(),
            authState = get(),
        )
    }
    viewModel {
        UsersViewModel(
            getUsersUseCase = get(),
            getUserUseCase = get(),
            registerUserUseCase = get(),
            updateUserUseCase = get(),
            assignRoleUseCase = get(),
            deleteUserUseCase = get(),
            getRolesUseCase = get(),
        )
    }
    viewModel {
        FleetTrackingViewModel(
            getActiveRoutesUseCase = get(),
            getFleetStatusUseCase = get(),
            fleetLiveClient = get(),
            trackingRepository = get(),
        )
    }
    viewModel {
        DriversViewModel(
            getDriversUseCase = get(),
            createDriverUseCase = get(),
            updateDriverUseCase = get(),
            activateDriverUseCase = get(),
            deactivateDriverUseCase = get(),
            assignDriverUseCase = get(),
            releaseDriverUseCase = get(),
            getVehiclesUseCase = get(),
            getVehicleUseCase = get(),
            driverRepository = get(),
            authState = get(),
        )
    }
}
