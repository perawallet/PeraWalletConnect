package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.crypto.codec.Codec
import app.perawallet.walletconnectv2.internal.common.crypto.sha256
import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.model.Namespace
import app.perawallet.walletconnectv2.internal.common.model.sync.ClientJsonRpc
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.internal.common.storage.push_messages.PushMessagesRepository
import app.perawallet.walletconnectv2.push.notifications.DecryptMessageUseCaseInterface
import app.perawallet.walletconnectv2.utils.toClient
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.sign.common.exceptions.InvalidSignParamsType
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.common.PayloadParams
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import org.bouncycastle.util.encoders.Base64

internal class DecryptSignMessageUseCase(
    private val codec: Codec,
    private val serializer: JsonRpcSerializer,
    private val metadataRepository: MetadataStorageRepositoryInterface,
    private val pushMessageStorage: PushMessagesRepository
) : DecryptMessageUseCaseInterface {
    override suspend fun decryptNotification(topic: String, message: String, onSuccess: (Core.Model.Message) -> Unit, onFailure: (Throwable) -> Unit) {
        try {
            if (!pushMessageStorage.doesPushMessageExist(sha256(message.toByteArray()))) {
                val decryptedMessageString = codec.decrypt(Topic(topic), Base64.decode(message))
                val clientJsonRpc: ClientJsonRpc = serializer.tryDeserialize<ClientJsonRpc>(decryptedMessageString) ?: return onFailure(InvalidSignParamsType())
                val params: ClientParams = serializer.deserialize(clientJsonRpc.method, decryptedMessageString) ?: return onFailure(InvalidSignParamsType())
                val metadata: AppMetaData = metadataRepository.getByTopicAndType(Topic(topic), AppMetaDataType.PEER) ?: return onFailure(InvalidSignParamsType())

                when (params) {
                    is SignParams.SessionProposeParams -> onSuccess(params.toCore(clientJsonRpc.id, topic))
                    is SignParams.SessionRequestParams -> onSuccess(params.toCore(clientJsonRpc.id, topic, metadata))
                    is SignParams.SessionAuthenticateParams -> onSuccess(params.toCore(clientJsonRpc.id, topic, metadata))
                    else -> onFailure(InvalidSignParamsType())
                }
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private companion object {
        fun SignParams.SessionProposeParams.toCore(id: Long, topic: String): Core.Model.Message.SessionProposal =
            Core.Model.Message.SessionProposal(
                id,
                topic,
                proposer.metadata.name,
                proposer.metadata.description,
                proposer.metadata.url,
                proposer.metadata.icons,
                proposer.metadata.redirect?.native ?: "",
                requiredNamespaces.toCore(),
                (optionalNamespaces ?: emptyMap()).toCore(),
                properties,
                proposer.publicKey,
                relays.first().protocol,
                relays.first().data
            )

        fun SignParams.SessionRequestParams.toCore(id: Long, topic: String, metaData: AppMetaData): Core.Model.Message.SessionRequest =
            Core.Model.Message.SessionRequest(
                topic,
                chainId,
                metaData.toClient(),
                Core.Model.Message.SessionRequest.JSONRPCRequest(id, request.method, request.params)
            )

        fun SignParams.SessionAuthenticateParams.toCore(id: Long, topic: String, metaData: AppMetaData): Core.Model.Message.SessionAuthenticate =
            Core.Model.Message.SessionAuthenticate(
                id,
                topic,
                metaData.toClient(),
                authPayload.toClient(),
                expiryTimestamp
            )

        fun PayloadParams.toClient(): Core.Model.Message.SessionAuthenticate.PayloadParams {
            return with(this) {
                Core.Model.Message.SessionAuthenticate.PayloadParams(chains, domain, nonce, aud, type, nbf, exp, statement, requestId, resources, iat)
            }
        }

        fun Map<String, Namespace.Proposal>.toCore(): Map<String, Core.Model.Namespace.Proposal> =
            mapValues { (_, namespace) -> Core.Model.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events) }
    }
}