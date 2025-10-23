package app.perawallet.walletconnectv2.internal.common.di

import com.squareup.moshi.Moshi
import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractor
import app.perawallet.walletconnectv2.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.json_rpc.domain.relay.RelayJsonRpcInteractor
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.model.type.SerializableJsonRpc
import app.perawallet.walletconnectv2.pairing.model.PairingJsonRpcMethod
import app.perawallet.walletconnectv2.pairing.model.PairingRpc
import app.perawallet.walletconnectv2.internal.utils.JsonAdapterEntry
import app.perawallet.walletconnectv2.internal.utils.addDeserializerEntry
import app.perawallet.walletconnectv2.internal.utils.addSerializerEntry
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.reflect.KClass

@JvmSynthetic
fun coreJsonRpcModule() = module {

    single<RelayJsonRpcInteractorInterface> {
        RelayJsonRpcInteractor(
            relay = get(),
            chaChaPolyCodec = get(),
            jsonRpcHistory = get(),
            pushMessageStorage = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
        )
    }

    addSerializerEntry(PairingRpc.PairingPing::class)
    addSerializerEntry(PairingRpc.PairingDelete::class)

    addDeserializerEntry(PairingJsonRpcMethod.WC_PAIRING_PING, PairingRpc.PairingPing::class)
    addDeserializerEntry(PairingJsonRpcMethod.WC_PAIRING_DELETE, PairingRpc.PairingDelete::class)

    factory {
        JsonRpcSerializer(
            serializerEntries = getAll<KClass<SerializableJsonRpc>>().toSet(),
            deserializerEntries = getAll<Pair<String, KClass<*>>>().toMap(),
            jsonAdapterEntries = getAll<JsonAdapterEntry<*>>().toSet(),
            moshiBuilder = get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI))
        )
    }

    single<LinkModeJsonRpcInteractorInterface> {
        LinkModeJsonRpcInteractor(
            chaChaPolyCodec = get(),
            jsonRpcHistory = get(),
            context = androidContext()
        )
    }
}