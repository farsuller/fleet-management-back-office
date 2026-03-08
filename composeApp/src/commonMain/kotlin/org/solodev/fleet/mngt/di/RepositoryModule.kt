package org.solodev.fleet.mngt.di

import org.koin.dsl.module
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.repository.AccountingRepository
import org.solodev.fleet.mngt.repository.AccountingRepositoryImpl
import org.solodev.fleet.mngt.repository.AuthRepository
import org.solodev.fleet.mngt.repository.AuthRepositoryImpl
import org.solodev.fleet.mngt.repository.CustomerRepository
import org.solodev.fleet.mngt.repository.CustomerRepositoryImpl
import org.solodev.fleet.mngt.repository.MaintenanceRepository
import org.solodev.fleet.mngt.repository.MaintenanceRepositoryImpl
import org.solodev.fleet.mngt.repository.RentalRepository
import org.solodev.fleet.mngt.repository.RentalRepositoryImpl
import org.solodev.fleet.mngt.repository.TrackingRepository
import org.solodev.fleet.mngt.repository.TrackingRepositoryImpl
import org.solodev.fleet.mngt.repository.VehicleRepository
import org.solodev.fleet.mngt.repository.VehicleRepositoryImpl

val repositoryModule = module {
    // Auth dispatcher acts as AuthState
    single { AppDependencyDispatcher(tokenProvider = get(), secureStorage = get()) }
    single<AuthState> { get<AppDependencyDispatcher>() }

    single<AuthRepository> { AuthRepositoryImpl(api = get(), dispatcher = get()) }
    single<VehicleRepository> { VehicleRepositoryImpl(api = get()) }
    single<RentalRepository> { RentalRepositoryImpl(api = get()) }
    single<CustomerRepository> { CustomerRepositoryImpl(api = get()) }
    single<MaintenanceRepository> { MaintenanceRepositoryImpl(api = get()) }
    single<AccountingRepository> { AccountingRepositoryImpl(api = get()) }
    single<TrackingRepository> { TrackingRepositoryImpl(api = get()) }
}
