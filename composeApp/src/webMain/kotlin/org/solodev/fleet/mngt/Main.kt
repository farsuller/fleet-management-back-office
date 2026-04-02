package org.solodev.fleet.mngt

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import eu.anifantakis.lib.ksafe.KSafe
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import org.solodev.fleet.mngt.auth.SecureStorage
import org.solodev.fleet.mngt.di.networkModule
import org.solodev.fleet.mngt.di.repositoryModule
import org.solodev.fleet.mngt.di.storageModule
import org.solodev.fleet.mngt.di.trackingModule
import org.solodev.fleet.mngt.di.useCaseModule
import org.solodev.fleet.mngt.di.viewModelModule
import org.solodev.fleet.mngt.navigation.RouteGuard
import org.solodev.fleet.mngt.platform.PlatformConfig
import org.solodev.fleet.mngt.theme.FleetTheme
import org.solodev.fleet.mngt.theme.ThemeState

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val koin = startKoin {
        logger(PrintLogger(Level.ERROR))
        properties(
            mapOf(
                "fleet.api.baseUrl" to PlatformConfig.apiBaseUrl,
                "fleet.ws.baseUrl" to PlatformConfig.wsBaseUrl,
            ),
        )
        modules(storageModule, networkModule, trackingModule, repositoryModule, useCaseModule, viewModelModule)
    }.koin

    ComposeViewport {
        KoinContext {
            var appReady by remember { mutableStateOf(false) }
            var themeState by remember { mutableStateOf(ThemeState(initialDark = true)) }

            // Wait for KSafe's WebCrypto cache to decrypt on WASM, then load saved theme
            LaunchedEffect(Unit) {
                koin.get<KSafe>().awaitCacheReady()
                val savedDark = koin.get<SecureStorage>().loadTheme() ?: true
                themeState = ThemeState(initialDark = savedDark)
                appReady = true
            }

            if (appReady) {
                // Persist theme preference whenever the user toggles it
                LaunchedEffect(themeState.isDark) {
                    koin.get<SecureStorage>().saveTheme(themeState.isDark)
                }
                FleetTheme(themeState = themeState) {
                    RouteGuard()
                }
            }
        }
    }
}
