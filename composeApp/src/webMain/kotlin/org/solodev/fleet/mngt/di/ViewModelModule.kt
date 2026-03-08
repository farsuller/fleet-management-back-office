package org.solodev.fleet.mngt.di

import org.koin.dsl.module
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
import org.solodev.fleet.mngt.domain.usecase.rental.GetPaymentMethodsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.PayInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.CreateVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.DeleteVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleLocationHistoryUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehiclesUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateOdometerUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateVehicleStateUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateVehicleUseCase
import org.solodev.fleet.mngt.features.auth.LoginViewModel
import org.solodev.fleet.mngt.features.customers.CustomersViewModel
import org.solodev.fleet.mngt.features.dashboard.DashboardViewModel
import org.solodev.fleet.mngt.features.rentals.RentalsViewModel
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
            getPaymentMethodsUseCase = get(),
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
}
