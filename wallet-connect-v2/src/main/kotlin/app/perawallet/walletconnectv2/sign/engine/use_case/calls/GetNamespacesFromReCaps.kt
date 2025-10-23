package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.model.Namespace
import app.perawallet.walletconnectv2.internal.utils.CoreValidator
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator

class GetNamespacesFromReCaps {
    operator fun invoke(chains: List<String>, methods: List<String>): Map<String, Namespace.Proposal> {
        if (!chains.all { chain -> CoreValidator.isChainIdCAIP2Compliant(chain) }) throw Exception("Chains are not CAIP-2 compliant")
        if (!chains.all { chain -> SignValidator.getNamespaceKeyFromChainId(chain) == EIP155 }) throw Exception("Only eip155 (EVM) is supported")
        val namespace = SignValidator.getNamespaceKeyFromChainId(chains.first())
        return mapOf(namespace to Namespace.Proposal(events = listOf("chainChanged", "accountsChanged"), methods = methods, chains = chains))
    }

    companion object {
        private const val EIP155 = "eip155"
    }
}