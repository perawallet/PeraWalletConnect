package app.perawallet.walletconnectv2.sign.common.model.vo.proposal

import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.Namespace
import app.perawallet.walletconnectv2.internal.common.model.Redirect
import app.perawallet.walletconnectv2.foundation.common.model.Topic

internal data class ProposalVO(
    val requestId: Long,
    val pairingTopic: Topic,
    val name: String,
    val description: String,
    val url: String,
    val icons: List<String>,
    val redirect: String,
    val requiredNamespaces: Map<String, Namespace.Proposal>,
    val optionalNamespaces: Map<String, Namespace.Proposal>,
    val properties: Map<String, String>?,
    val proposerPublicKey: String,
    val relayProtocol: String,
    val relayData: String?,
    val expiry: Expiry?
) {
    val appMetaData: AppMetaData
        get() = AppMetaData(name = name, description = description, url = url, icons = icons, redirect = Redirect(native = redirect))
}