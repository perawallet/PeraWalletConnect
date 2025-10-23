package app.perawallet.walletconnectv2.auth.use_case.calls

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.crypto.codec.Codec
import app.perawallet.walletconnectv2.internal.common.crypto.sha256
import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.model.sync.ClientJsonRpc
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.internal.common.storage.push_messages.PushMessagesRepository
import app.perawallet.walletconnectv2.push.notifications.DecryptMessageUseCaseInterface
import app.perawallet.walletconnectv2.utils.toClient
import app.perawallet.walletconnectv2.auth.common.exceptions.InvalidAuthParamsType
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthParams
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import org.bouncycastle.util.encoders.Base64

class DecryptAuthMessageUseCase(
    private val codec: Codec,
    private val serializer: JsonRpcSerializer,
    private val metadataRepository: MetadataStorageRepositoryInterface,
    private val pushMessageStorageRepository: PushMessagesRepository
) : DecryptMessageUseCaseInterface {
    override suspend fun decryptNotification(topic: String, message: String, onSuccess: (Core.Model.Message) -> Unit, onFailure: (Throwable) -> Unit) {
        try {
            if (!pushMessageStorageRepository.doesPushMessageExist(sha256(message.toByteArray()))) {
                val decryptedMessageString = codec.decrypt(Topic(topic), Base64.decode(message))
                val clientJsonRpc: ClientJsonRpc = serializer.tryDeserialize<ClientJsonRpc>(decryptedMessageString) ?: return onFailure(InvalidAuthParamsType())
                val params: ClientParams = serializer.deserialize(clientJsonRpc.method, decryptedMessageString) ?: return onFailure(InvalidAuthParamsType())
                val metadata: AppMetaData = metadataRepository.getByTopicAndType(Topic(topic), AppMetaDataType.PEER) ?: return onFailure(InvalidAuthParamsType())

                if (params is AuthParams.RequestParams) {
                    onSuccess(params.toMessage(clientJsonRpc.id, topic, metadata))
                } else {
                    onFailure(InvalidAuthParamsType())
                }
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private fun AuthParams.RequestParams.toMessage(id: Long, topic: String, metadata: AppMetaData): Core.Model.Message.AuthRequest = with(payloadParams) {
        Core.Model.Message.AuthRequest(
            id,
            topic,
            metadata.toClient(),
            Core.Model.Message.AuthRequest.PayloadParams(type, chainId, domain, aud, version, nonce, iat, nbf, exp, statement, requestId, resources)
        )
    }
}