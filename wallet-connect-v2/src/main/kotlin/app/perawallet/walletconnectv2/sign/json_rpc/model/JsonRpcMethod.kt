

package app.perawallet.walletconnectv2.sign.json_rpc.model

internal object JsonRpcMethod {
    
    const val WC_SESSION_PROPOSE: String = "wc_sessionPropose"
    
    const val WC_SESSION_AUTHENTICATE: String = "wc_sessionAuthenticate"
    
    const val WC_SESSION_SETTLE: String = "wc_sessionSettle"
    
    const val WC_SESSION_REQUEST: String = "wc_sessionRequest"
    
    const val WC_SESSION_DELETE: String = "wc_sessionDelete"
    
    const val WC_SESSION_PING: String = "wc_sessionPing"
    
    const val WC_SESSION_EVENT: String = "wc_sessionEvent"
    
    const val WC_SESSION_UPDATE: String = "wc_sessionUpdate"
    
    const val WC_SESSION_EXTEND: String = "wc_sessionExtend"
}