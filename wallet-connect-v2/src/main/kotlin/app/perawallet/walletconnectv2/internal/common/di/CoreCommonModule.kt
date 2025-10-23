package app.perawallet.walletconnectv2.internal.common.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.tinder.scarlet.utils.getRawType
import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.adapter.ExpiryAdapter
import app.perawallet.walletconnectv2.internal.common.adapter.JsonRpcResultAdapter
import app.perawallet.walletconnectv2.internal.common.adapter.TagsAdapter
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.di.FoundationDITags
import app.perawallet.walletconnectv2.foundation.di.foundationCommonModule
import app.perawallet.walletconnectv2.foundation.util.Logger
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber
import kotlin.reflect.jvm.jvmName

fun coreCommonModule() = module {

    includes(foundationCommonModule())

    single<PolymorphicJsonAdapterFactory<JsonRpcResponse>> {
        PolymorphicJsonAdapterFactory.of(JsonRpcResponse::class.java, "type")
            .withSubtype(JsonRpcResponse.JsonRpcResult::class.java, "result")
            .withSubtype(JsonRpcResponse.JsonRpcError::class.java, "error")
    }

    single<Moshi.Builder>(named(AndroidCommonDITags.MOSHI)) {
        get<Moshi>(named(FoundationDITags.MOSHI))
            .newBuilder()
            .add { type, _, moshi ->
                when (type.getRawType().name) {
                    Expiry::class.jvmName -> ExpiryAdapter
                    Tags::class.jvmName -> TagsAdapter
                    JsonRpcResponse.JsonRpcResult::class.jvmName -> JsonRpcResultAdapter(moshi)
                    else -> null
                }
            }
            .add(get<PolymorphicJsonAdapterFactory<JsonRpcResponse>>())
            .add(get<PolymorphicJsonAdapterFactory<Props>>())
    }

    single {
        Timber
    }

    single<Logger>(named(AndroidCommonDITags.LOGGER)) {
        object : Logger {
            override fun log(logMsg: String?) {
                get<Timber.Forest>().d(logMsg)
            }

            override fun log(throwable: Throwable?) {
                get<Timber.Forest>().d(throwable)
            }

            override fun error(errorMsg: String?) {
                get<Timber.Forest>().e(errorMsg)
            }

            override fun error(throwable: Throwable?) {
                get<Timber.Forest>().e(throwable)
            }
        }
    }
}