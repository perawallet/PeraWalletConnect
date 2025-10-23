package app.perawallet.walletconnectv2.internal.common.di

import com.squareup.moshi.Moshi
import app.perawallet.walletconnectv2.internal.common.model.TelemetryEnabled
import app.perawallet.walletconnectv2.pulse.data.PulseService
import app.perawallet.walletconnectv2.pulse.domain.InsertEventUseCase
import app.perawallet.walletconnectv2.pulse.domain.InsertTelemetryEventUseCase
import app.perawallet.walletconnectv2.pulse.domain.SendBatchEventUseCase
import app.perawallet.walletconnectv2.pulse.domain.SendEventInterface
import app.perawallet.walletconnectv2.pulse.domain.SendEventUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


fun pulseModule(bundleId: String) = module {
    single(named(AndroidCommonDITags.PULSE_URL)) { "https://pulse.walletconnect.org" }

    single(named(AndroidCommonDITags.PULSE_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl(get<String>(named(AndroidCommonDITags.PULSE_URL)))
            .client(get(named(AndroidCommonDITags.WEB3MODAL_OKHTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI)).build()))
            .build()
    }

    single {
        get<Retrofit>(named(AndroidCommonDITags.PULSE_RETROFIT)).create(PulseService::class.java)
    }

    single<SendEventInterface> {
        SendEventUseCase(
            pulseService = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            bundleId = bundleId
        )
    }

    single {
        SendBatchEventUseCase(
            pulseService = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            telemetryEnabled = get<TelemetryEnabled>(named(AndroidCommonDITags.TELEMETRY_ENABLED)),
            eventsRepository = get(),
        )
    }

    single {
        InsertTelemetryEventUseCase(
            logger = get(named(AndroidCommonDITags.LOGGER)),
            eventsRepository = get(),
        )
    }

    single {
        InsertEventUseCase(
            logger = get(named(AndroidCommonDITags.LOGGER)),
            eventsRepository = get(),
        )
    }
}