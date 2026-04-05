package org.solodev.fleet.mngt.di

import eu.anifantakis.lib.ksafe.KSafe
import org.koin.dsl.module
import org.solodev.fleet.mngt.auth.SecureStorage

val storageModule =
    module {
        single { SecureStorage() }
        // Expose the KSafe instance so Main.kt can call awaitCacheReady()
        single<KSafe> { get<SecureStorage>().ksafe }
    }
