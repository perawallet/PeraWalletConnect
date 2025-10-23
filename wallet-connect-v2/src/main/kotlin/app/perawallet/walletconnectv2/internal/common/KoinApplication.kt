package app.perawallet.walletconnectv2.internal.common

import org.koin.core.KoinApplication

var wcKoinApp: KoinApplication = KoinApplication.init().apply { createEagerInstances() }