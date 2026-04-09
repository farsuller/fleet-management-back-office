package org.solodev.fleet.mngt.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.solodev.fleet.mngt.domain.usecase.accounting.CreateInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetAccountsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetAllDriverPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetDriverPendingPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoicesByCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetInvoicesUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentMethodsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentsByCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetRemittanceUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.GetRemittancesByDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.RecordDriverCollectionUseCase
import org.solodev.fleet.mngt.domain.usecase.accounting.SubmitRemittanceUseCase
import org.solodev.fleet.mngt.domain.usecase.auth.LoginUseCase
import org.solodev.fleet.mngt.domain.usecase.auth.LogoutUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.CreateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.DeactivateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerPaymentsUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.GetCustomersUseCase
import org.solodev.fleet.mngt.domain.usecase.customer.UpdateCustomerUseCase
import org.solodev.fleet.mngt.domain.usecase.dashboard.GetDashboardUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.ActivateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.AssignDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.CreateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.DeactivateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetDriverAssignmentsUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetDriversUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetVehicleActiveDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.GetVehicleDriverHistoryUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.ReleaseDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.driver.UpdateDriverUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.CancelMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.CompleteMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.GetMaintenanceJobUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.GetMaintenanceJobsUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.ScheduleMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.maintenance.StartMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.ActivateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CancelRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CompleteRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.CreateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.DeleteRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetRentalsUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.PayInvoiceUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.UpdateRentalUseCase
import org.solodev.fleet.mngt.domain.usecase.tracking.GetActiveRoutesUseCase
import org.solodev.fleet.mngt.domain.usecase.tracking.GetFleetStatusUseCase
import org.solodev.fleet.mngt.domain.usecase.tracking.GetVehicleStateUseCase
import org.solodev.fleet.mngt.domain.usecase.user.AssignRoleUseCase
import org.solodev.fleet.mngt.domain.usecase.user.DeleteUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetRolesUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.GetUsersUseCase
import org.solodev.fleet.mngt.domain.usecase.user.RegisterUserUseCase
import org.solodev.fleet.mngt.domain.usecase.user.UpdateUserUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.CreateVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.DeleteVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleIncidentsUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleLocationHistoryUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleMaintenanceUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.GetVehiclesUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateOdometerUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateVehicleStateUseCase
import org.solodev.fleet.mngt.domain.usecase.vehicle.UpdateVehicleUseCase
import org.solodev.fleet.mngt.domain.usecase.rental.GetPaymentMethodsUseCase as RentalGetPaymentMethodsUseCase

val useCaseModule =
    module {
        // Auth
        factory { LoginUseCase(get()) }
        factory { LogoutUseCase(get()) }

        // Vehicle
        factory { GetVehiclesUseCase(get()) }
        factory { GetVehicleUseCase(get()) }
        factory { CreateVehicleUseCase(get()) }
        factory { UpdateVehicleUseCase(get()) }
        factory { UpdateVehicleStateUseCase(get(), get()) }
        factory { UpdateOdometerUseCase(get()) }
        factory { DeleteVehicleUseCase(get()) }
        factory { GetVehicleMaintenanceUseCase(get()) }
        factory { GetVehicleLocationHistoryUseCase(get()) }

        // Customer
        factory { GetCustomersUseCase(get()) }
        factory { GetCustomerUseCase(get()) }
        factory { CreateCustomerUseCase(get()) }
        factory { UpdateCustomerUseCase(get()) }
        factory { DeactivateCustomerUseCase(get()) }
        factory { GetCustomerRentalsUseCase(get()) }
        factory { GetCustomerPaymentsUseCase(get()) }

        // Driver
        factory { GetDriversUseCase(get()) }
        factory { GetDriverUseCase(get()) }
        factory { CreateDriverUseCase(get()) }
        factory { ActivateDriverUseCase(get()) }
        factory { DeactivateDriverUseCase(get()) }
        factory { AssignDriverUseCase(get()) }
        factory { ReleaseDriverUseCase(get()) }
        factory { GetDriverAssignmentsUseCase(get()) }
        factory { GetVehicleActiveDriverUseCase(get()) }
        factory { GetVehicleDriverHistoryUseCase(get()) }
        factory { UpdateDriverUseCase(get()) }

        // Rental
        factory { GetRentalsUseCase(get()) }
        factory { GetRentalUseCase(get()) }
        factory { CreateRentalUseCase(get()) }
        factory { UpdateRentalUseCase(get()) }
        factory { ActivateRentalUseCase(get(), get()) }
        factory { CancelRentalUseCase(get()) }
        factory { CompleteRentalUseCase(get(), get()) }
        factory { DeleteRentalUseCase(get()) }
        factory(named("rentalPaymentMethods")) { RentalGetPaymentMethodsUseCase(get()) }
        factory { PayInvoiceUseCase(get()) }

        // Accounting
        factory { GetInvoicesUseCase(get()) }
        factory { GetInvoiceUseCase(get()) }
        factory { GetInvoicesByCustomerUseCase(get()) }
        factory { CreateInvoiceUseCase(get()) }
        factory { GetPaymentsUseCase(get()) }
        factory { GetPaymentsByCustomerUseCase(get()) }
        factory { GetAccountsUseCase(get()) }
        factory { GetPaymentMethodsUseCase(get()) }
        factory { RecordDriverCollectionUseCase(get()) }
        factory { GetDriverPendingPaymentsUseCase(get()) }
        factory { GetAllDriverPaymentsUseCase(get()) }
        factory { SubmitRemittanceUseCase(get()) }
        factory { GetRemittancesByDriverUseCase(get()) }
        factory { GetRemittanceUseCase(get()) }

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
        factory { RegisterUserUseCase(get()) }
        factory { UpdateUserUseCase(get()) }
        factory { AssignRoleUseCase(get()) }
        factory { GetRolesUseCase(get()) }
        factory { DeleteUserUseCase(get()) }

        // Dashboard
        factory { GetDashboardUseCase(get(), get(), get(), get()) }

        // Tracking
        factory { GetFleetStatusUseCase(get()) }
        factory { GetActiveRoutesUseCase(get()) }
        factory { GetVehicleStateUseCase(get()) }

        // Incident
        factory { GetVehicleIncidentsUseCase(get()) }
    }
