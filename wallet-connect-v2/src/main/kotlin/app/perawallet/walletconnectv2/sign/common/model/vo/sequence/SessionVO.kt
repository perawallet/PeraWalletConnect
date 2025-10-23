@file:JvmSynthetic

package app.perawallet.walletconnectv2.sign.common.model.vo.sequence

import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.Namespace
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.internal.common.model.type.Sequence
import app.perawallet.walletconnectv2.internal.utils.ACTIVE_SESSION
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.common.SessionParticipant
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.model.vo.proposal.ProposalVO
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toMapOfNamespacesVOSession

internal data class SessionVO(
    override val topic: Topic,
    override val expiry: Expiry,
    val relayProtocol: String,
    val relayData: String?,
    val controllerKey: PublicKey? = null,
    val selfPublicKey: PublicKey,
    val selfAppMetaData: AppMetaData? = null,
    val peerPublicKey: PublicKey? = null,
    val peerAppMetaData: AppMetaData? = null,
    val sessionNamespaces: Map<String, Namespace.Session>,
    val requiredNamespaces: Map<String, Namespace.Proposal>,
    val optionalNamespaces: Map<String, Namespace.Proposal>?,
    val properties: Map<String, String>? = null,
    val isAcknowledged: Boolean,
    val pairingTopic: String,
    val transportType: TransportType?
) : Sequence {
    val isPeerController: Boolean = peerPublicKey?.keyAsHex == controllerKey?.keyAsHex
    val isSelfController: Boolean = selfPublicKey.keyAsHex == controllerKey?.keyAsHex
    val peerLinkMode: Boolean? = peerAppMetaData?.redirect?.linkMode
    val peerAppLink: String? = peerAppMetaData?.redirect?.universal

    internal companion object {

        @JvmSynthetic
        internal fun createUnacknowledgedSession(
            sessionTopic: Topic,
            proposal: ProposalVO,
            selfParticipant: SessionParticipant,
            sessionExpiry: Long,
            namespaces: Map<String, EngineDO.Namespace.Session>,
            pairingTopic: String
        ): SessionVO {
            return SessionVO(
                sessionTopic,
                Expiry(sessionExpiry),
                relayProtocol = proposal.relayProtocol,
                relayData = proposal.relayData,
                peerPublicKey = PublicKey(proposal.proposerPublicKey),
                peerAppMetaData = proposal.appMetaData,
                selfPublicKey = PublicKey(selfParticipant.publicKey),
                selfAppMetaData = selfParticipant.metadata,
                controllerKey = PublicKey(selfParticipant.publicKey),
                sessionNamespaces = namespaces.toMapOfNamespacesVOSession(),
                requiredNamespaces = proposal.requiredNamespaces,
                optionalNamespaces = proposal.optionalNamespaces,
                properties = proposal.properties,
                isAcknowledged = false,
                pairingTopic = pairingTopic,
                transportType = TransportType.RELAY
            )
        }

        @JvmSynthetic
        internal fun createAcknowledgedSession(
            sessionTopic: Topic,
            settleParams: SignParams.SessionSettleParams,
            selfPublicKey: PublicKey,
            selfMetadata: AppMetaData,
            requiredNamespaces: Map<String, Namespace.Proposal>,
            optionalNamespaces: Map<String, Namespace.Proposal>?,
            properties: Map<String, String>?,
            pairingTopic: String
        ): SessionVO {
            return SessionVO(
                sessionTopic,
                Expiry(settleParams.expiry),
                relayProtocol = settleParams.relay.protocol,
                relayData = settleParams.relay.data,
                peerPublicKey = PublicKey(settleParams.controller.publicKey),
                peerAppMetaData = settleParams.controller.metadata,
                selfPublicKey = selfPublicKey,
                selfAppMetaData = selfMetadata,
                controllerKey = PublicKey(settleParams.controller.publicKey),
                sessionNamespaces = settleParams.namespaces,
                requiredNamespaces = requiredNamespaces,
                optionalNamespaces = optionalNamespaces,
                properties = properties,
                isAcknowledged = true,
                pairingTopic = pairingTopic,
                transportType = TransportType.RELAY
            )
        }

        @JvmSynthetic
        internal fun createAuthenticatedSession(
            sessionTopic: Topic,
            peerPublicKey: PublicKey,
            peerMetadata: AppMetaData,
            selfPublicKey: PublicKey,
            selfMetadata: AppMetaData,
            controllerKey: PublicKey?,
            requiredNamespaces: Map<String, Namespace.Proposal>,
            sessionNamespaces: Map<String, Namespace.Session>,
            pairingTopic: String,
            transportType: TransportType?
        ): SessionVO {
            return SessionVO(
                sessionTopic,
                Expiry(ACTIVE_SESSION),
                relayProtocol = "irn",
                relayData = null,
                peerPublicKey = peerPublicKey,
                peerAppMetaData = peerMetadata,
                selfPublicKey = selfPublicKey,
                selfAppMetaData = selfMetadata,
                controllerKey = controllerKey,
                sessionNamespaces = sessionNamespaces,
                requiredNamespaces = requiredNamespaces,
                optionalNamespaces = null,
                isAcknowledged = true,
                pairingTopic = pairingTopic,
                transportType = transportType
            )
        }
    }
}