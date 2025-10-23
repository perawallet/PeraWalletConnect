@file:JvmSynthetic

package app.perawallet.walletconnectv2.auth.common.json_rpc

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.model.type.JsonRpcClientSync
import app.perawallet.walletconnectv2.auth.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.foundation.util.generateId

internal sealed class AuthRpc : JsonRpcClientSync<AuthParams> {

    @JsonClass(generateAdapter = true)
    internal data class AuthRequest(
        @Json(name = "id")
        override val id: Long = generateId(),
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = JsonRpcMethod.WC_AUTH_REQUEST,
        @Json(name = "params")
        override val params: AuthParams.RequestParams
    ) : AuthRpc()
}