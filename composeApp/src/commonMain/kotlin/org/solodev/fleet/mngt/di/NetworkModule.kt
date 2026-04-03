package org.solodev.fleet.mngt.di

import org.koin.dsl.module
import org.solodev.fleet.mngt.api.FleetApiClient
import org.solodev.fleet.mngt.auth.AuthState
import org.solodev.fleet.mngt.auth.InMemoryTokenProvider
import org.solodev.fleet.mngt.auth.TokenProvider
import org.solodev.fleet.mngt.tracking.FleetLiveClient

val networkModule = module {
    single<TokenProvider> { InMemoryTokenProvider() }
    single {
        FleetApiClient(
            baseUrl = getProperty("fleet.api.baseUrl"),
            tokenProvider = get(),
            authState = get<AuthState>(),
        )
    }
}

val trackingModule = module {
    single {
        FleetLiveClient(
            wsBaseUrl = getProperty("fleet.ws.baseUrl"),
            tokenProvider = get(),
        )
    }
}
