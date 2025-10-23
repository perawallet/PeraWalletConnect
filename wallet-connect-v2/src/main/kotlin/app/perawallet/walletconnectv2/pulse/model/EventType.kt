package app.perawallet.walletconnectv2.pulse.model

object EventType {
    
    const val ERROR: String = "ERROR"
    
    const val SUCCESS: String = "SUCCESS"

    
    const val TRACK: String = "TRACE"

    object Error {
        
        const val NO_WSS_CONNECTION: String = "NO_WSS_CONNECTION"

        
        const val NO_INTERNET_CONNECTION: String = "NO_INTERNET_CONNECTION"

        
        const val MALFORMED_PAIRING_URI: String = "MALFORMED_PAIRING_URI"

        
        const val PAIRING_ALREADY_EXIST: String = "PAIRING_ALREADY_EXIST"

        
        const val PAIRING_SUBSCRIPTION_FAILURE: String = "FAILED_TO_SUBSCRIBE_TO_PAIRING_TOPIC"

        
        const val PAIRING_URI_EXPIRED: String = "PAIRING_URI_EXPIRED"

        
        const val PAIRING_EXPIRED: String = "PAIRING_EXPIRED"

        
        const val PROPOSAL_EXPIRED: String = "PROPOSAL_EXPIRED"

        
        const val SESSION_SUBSCRIPTION_FAILURE: String = "SESSION_SUBSCRIPTION_FAILURE"

        
        const val SESSION_APPROVE_PUBLISH_FAILURE: String = "SESSION_APPROVE_PUBLISH_FAILURE"

        
        const val SESSION_SETTLE_PUBLISH_FAILURE: String = "SESSION_SETTLE_PUBLISH_FAILURE"

        
        const val SESSION_APPROVE_NAMESPACE_VALIDATION_FAILURE: String = "SESSION_APPROVE_NAMESPACE_VALIDATION_FAILURE"

        
        const val REQUIRED_NAMESPACE_VALIDATION_FAILURE: String = "REQUIRED_NAMESPACE_VALIDATION_FAILURE"

        
        const val OPTIONAL_NAMESPACE_VALIDATION_FAILURE: String = "OPTIONAL_NAMESPACE_VALIDATION_FAILURE"

        
        const val SESSION_PROPERTIES_VALIDATION_FAILURE: String = "SESSION_PROPERTIES_VALIDATION_FAILURE"

        
        const val MISSING_SESSION_AUTH_REQUEST: String = "MISSING_SESSION_AUTH_REQUEST"

        
        const val SESSION_AUTH_REQUEST_EXPIRED: String = "SESSION_AUTH_REQUEST_EXPIRED"

        
        const val CHAINS_CAIP2_COMPLIANT_FAILURE: String = "CHAINS_CAIP2_COMPLIANT_FAILURE"

        
        const val CHAINS_EVM_COMPLIANT_FAILURE: String = "CHAINS_EVM_COMPLIANT_FAILURE"

        
        const val INVALID_CACAO: String = "INVALID_CACAO"

        
        const val SUBSCRIBE_AUTH_SESSION_TOPIC_FAILURE: String = "SUBSCRIBE_AUTH_SESSION_TOPIC_FAILURE"

        
        const val AUTHENTICATED_SESSION_APPROVE_PUBLISH_FAILURE: String = "AUTHENTICATED_SESSION_APPROVE_PUBLISH_FAILURE"

        
        const val AUTHENTICATED_SESSION_EXPIRED: String = "AUTHENTICATED_SESSION_EXPIRED"
    }

    object Track {
        
        const val MODAL_CREATED: String = "MODAL_CREATED"

        
        const val MODAL_LOADED: String = "MODAL_LOADED"

        
        const val MODAL_OPEN: String = "MODAL_OPEN"

        
        const val MODAL_CLOSE: String = "MODAL_CLOSE"

        
        const val CLICK_ALL_WALLETS: String = "CLICK_ALL_WALLETS"

        
        const val CLICK_NETWORKS: String = "CLICK_NETWORKS"

        
        const val SWITCH_NETWORK: String = "SWITCH_NETWORK"

        
        const val SELECT_WALLET: String = "SELECT_WALLET"

        
        const val CONNECT_SUCCESS: String = "CONNECT_SUCCESS"

        
        const val CONNECT_ERROR: String = "CONNECT_ERROR"

        
        const val DISCONNECT_SUCCESS: String = "DISCONNECT_SUCCESS"

        
        const val DISCONNECT_ERROR: String = "DISCONNECT_ERROR"

        
        const val CLICK_WALLET_HELP: String = "CLICK_WALLET_HELP"

        
        const val CLICK_NETWORK_HELP: String = "CLICK_NETWORK_HELP"

        
        const val CLICK_GET_WALLET: String = "CLICK_GET_WALLET"
    }
}