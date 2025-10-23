package app.perawallet.walletconnectv2.auth.di

import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthRpc
import app.perawallet.walletconnectv2.auth.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.internal.utils.addDeserializerEntry
import app.perawallet.walletconnectv2.internal.utils.addSerializerEntry
import org.koin.dsl.module

@JvmSynthetic
internal fun jsonRpcModule() = module {

    addSerializerEntry(AuthRpc.AuthRequest::class)

    addDeserializerEntry(JsonRpcMethod.WC_AUTH_REQUEST, AuthRpc.AuthRequest::class)
}