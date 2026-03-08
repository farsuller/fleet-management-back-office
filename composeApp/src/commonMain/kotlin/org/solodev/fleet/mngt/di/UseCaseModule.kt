package org.solodev.fleet.mngt.di

import org.koin.dsl.module
import org.solodev.fleet.mngt.domain.usecase.auth.LoginUseCase
import org.solodev.fleet.mngt.domain.usecase.auth.LogoutUseCase
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

val useCaseModule = module {
    // Auth
    factory { LoginUseCase(get()) }
    factory { LogoutUseCase(get()) }

    // Vehicle
    factory { GetVehiclesUseCase(get()) }
    factory { GetVehicleUseCase(get()) }
    factory { CreateVehicleUseCase(get()) }
    factory { UpdateVehicleUseCase(get()) }
    factory { UpdateVehicleStateUseCase(get()) }
    factory { UpdateOdometerUseCase(get()) }
    factory { DeleteVehicleUseCase(get()) }
    factory { GetVehicleMaintenanceUseCase(get()) }
    factory { GetVehicleLocationHistoryUseCase(get()) }

    // Customer
    factory { GetCustomersUseCase(get()) }
    factory { GetCustomerUseCase(get()) }
    factory { CreateCustomerUseCase(get()) }
    factory { DeactivateCustomerUseCase(get()) }
    factory { GetCustomerRentalsUseCase(get()) }
    factory { GetCustomerPaymentsUseCase(get()) }

    // Rental
    factory { GetRentalsUseCase(get()) }
    factory { GetRentalUseCase(get()) }
    factory { CreateRentalUseCase(get()) }
    factory { ActivateRentalUseCase(get()) }
    factory { CancelRentalUseCase(get()) }
    factory { CompleteRentalUseCase(get()) }
    factory { GetPaymentMethodsUseCase(get()) }
    factory { PayInvoiceUseCase(get()) }

    // Dashboard
    factory { GetDashboardUseCase(get(), get(), get(), get()) }
}
