package org.solodev.fleet.mngt.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.solodev.fleet.mngt.domain.usecase.accounting.GetAccountsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoicesUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.CreateInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentsByCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentMethodsUseCase
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
import org.solodev.fleet.mngt.domain.usecase.maintenance.CancelMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.CompleteMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.GetMaintenanceJobUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.GetMaintenanceJobsUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.ScheduleMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.StartMaintenanceUseCase
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
    factory(named("rentalPaymentMethods")) { RentalGetPaymentMethodsUseCase(get()) }
    factory { PayInvoiceUseCase(get()) }

    // Accounting
    factory { GetInvoicesUseCase(get()) }
    factory { GetInvoiceUseCase(get()) }
    factory { CreateInvoiceUseCase(get()) }
    factory { GetPaymentsUseCase(get()) }
    factory { GetPaymentsByCustomerUseCase(get()) }
    factory { GetAccountsUseCase(get()) }
    factory { GetPaymentMethodsUseCase(get()) }

    // Maintenance
    factory { GetMaintenanceJobsUseCase(get()) }
    factory { GetMaintenanceJobUseCase(get()) }
    factory { ScheduleMaintenanceUseCase(get()) }
    factory { StartMaintenanceUseCase(get()) }
    factory { CompleteMaintenanceUseCase(get()) }
    factory { CancelMaintenanceUseCase(get()) }

    // Users
    factory { GetUsersUseCase(get()) }
    factory { GetUserUseCase(get()) }
    factory { AssignRolesUseCase(get()) }
    factory { DeleteUserUseCase(get()) }

    // Dashboard
    factory { GetDashboardUseCase(get(), get(), get(), get()) }
}
