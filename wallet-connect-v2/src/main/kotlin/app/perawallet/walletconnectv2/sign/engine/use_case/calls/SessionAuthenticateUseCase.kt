package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import android.util.Base64
import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.exception.InvalidExpiryException
import app.perawallet.walletconnectv2.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.EnvelopeType
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao.Payload.Companion.ATT_KEY
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao.Payload.Companion.RECAPS_PREFIX
import app.perawallet.walletconnectv2.internal.common.signing.cacao.mergeReCaps
import app.perawallet.walletconnectv2.internal.utils.CoreValidator
import app.perawallet.walletconnectv2.internal.utils.currentTimeInSeconds
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.internal.utils.getParticipantTag
import app.perawallet.walletconnectv2.internal.utils.oneHourInSeconds
import app.perawallet.walletconnectv2.pairing.model.mapper.toPairing
import app.perawallet.walletconnectv2.pulse.domain.InsertEventUseCase
import app.perawallet.walletconnectv2.pulse.model.Direction
import app.perawallet.walletconnectv2.pulse.model.EventType
import app.perawallet.walletconnectv2.pulse.model.properties.Properties
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.common.Requester
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toCommon
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toMapOfEngineNamespacesOptional
import app.perawallet.walletconnectv2.sign.storage.authenticate.AuthenticateResponseTopicRepository
import app.perawallet.walletconnectv2.sign.storage.link_mode.LinkModeStorageRepository
import app.perawallet.walletconnectv2.internal.utils.Empty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal class SessionAuthenticateUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val crypto: KeyManagementRepository,
    private val selfAppMetaData: AppMetaData,
    private val authenticateResponseTopicRepository: AuthenticateResponseTopicRepository,
    private val proposeSessionUseCase: ProposeSessionUseCaseInterface,
    private val getPairingForSessionAuthenticate: GetPairingForSessionAuthenticateUseCase,
    private val getNamespacesFromReCaps: GetNamespacesFromReCaps,
    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface,
    private val linkModeStorageRepository: LinkModeStorageRepository,
    private val insertEventUseCase: InsertEventUseCase,
    private val clientId: String,
    private val logger: Logger
) : SessionAuthenticateUseCaseInterface {
    override suspend fun authenticate(
        authenticate: EngineDO.Authenticate,
        methods: List<String>?,
        pairingTopic: String?,
        expiry: Expiry?,
        walletAppLink: String?,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (authenticate.chains.isEmpty()) {
            logger.error("Sending session authenticate request error: chains are empty")
            return onFailure(IllegalArgumentException("Chains are empty"))
        }

        if (!CoreValidator.isExpiryWithinBounds(expiry)) {
            logger.error("Sending session authenticate request error: expiry not within bounds")
            return onFailure(InvalidExpiryException())
        }

        val requestExpiry = expiry ?: Expiry(currentTimeInSeconds + oneHourInSeconds)
        val optionalNamespaces = getNamespacesFromReCaps(authenticate.chains, if (methods.isNullOrEmpty()) listOf("personal_sign") else methods).toMapOfEngineNamespacesOptional()
        val externalReCapsJson: String = getExternalReCapsJson(authenticate)
        val signReCapsJson = getSignReCapsJson(methods, authenticate)

        val reCaps = when {
            externalReCapsJson.isNotEmpty() && signReCapsJson.isNotEmpty() -> mergeReCaps(JSONObject(signReCapsJson), JSONObject(externalReCapsJson))
            signReCapsJson.isNotEmpty() -> signReCapsJson
            else -> externalReCapsJson
        }.replace("\\\\/", "/")

        if (reCaps.isNotEmpty()) {
            val base64Recaps = Base64.encodeToString(reCaps.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.NO_PADDING)
            val reCapsUrl = "$RECAPS_PREFIX$base64Recaps"
            if (authenticate.resources == null) authenticate.resources = listOf(reCapsUrl) else authenticate.resources = authenticate.resources!! + reCapsUrl
        }

        val requesterPublicKey: PublicKey = crypto.generateAndStoreX25519KeyPair()
        val responseTopic: Topic = crypto.getTopicFromKey(requesterPublicKey)
        val authParams: SignParams.SessionAuthenticateParams =
            SignParams.SessionAuthenticateParams(Requester(requesterPublicKey.keyAsHex, selfAppMetaData), authenticate.toCommon(), expiryTimestamp = requestExpiry.seconds)
        val authRequest: SignRpc.SessionAuthenticate = SignRpc.SessionAuthenticate(params = authParams)
        crypto.setKey(requesterPublicKey, responseTopic.getParticipantTag())

        if (isLinkModeEnabled(walletAppLink)) {
            try {
                linkModeJsonRpcInteractor.triggerRequest(authRequest, appLink = walletAppLink!!, topic = Topic(generateUUID()), envelopeType = EnvelopeType.TWO)
                insertEventUseCase(
                    Props(
                        EventType.SUCCESS,
                        Tags.SESSION_AUTHENTICATE_LINK_MODE.id.toString(),
                        Properties(correlationId = authRequest.id, clientId = clientId, direction = Direction.SENT.state)
                    )
                )
                logger.log("Link Mode - Request triggered successfully")
            } catch (e: Error) {
                onFailure(e)
            }
        } else {
            val pairing = getPairingForSessionAuthenticate(pairingTopic)
            logger.log("Session authenticate subscribing on topic: $responseTopic")
            jsonRpcInteractor.subscribe(
                responseTopic,
                onSuccess = {
                    logger.log("Session authenticate subscribed on topic: $responseTopic")
                    scope.launch {
                        authenticateResponseTopicRepository.insertOrAbort(pairing.topic, responseTopic.value)
                    }
                },
                onFailure = { error ->
                    logger.error("Session authenticate subscribing on topic error: $responseTopic, $error")
                    onFailure(error)
                })

            scope.launch {
                supervisorScope {
                    val sessionAuthenticateDeferred = publishSessionAuthenticateDeferred(pairing, authRequest, responseTopic, requestExpiry)
                    val sessionProposeDeferred = publishSessionProposeDeferred(pairing, optionalNamespaces, responseTopic)

                    val sessionAuthenticateResult = async { sessionAuthenticateDeferred }.await()
                    val sessionProposeResult = async { sessionProposeDeferred }.await()

                    when {
                        sessionAuthenticateResult.isSuccess && sessionProposeResult.isSuccess -> onSuccess(pairing.uri)
                        sessionAuthenticateResult.isFailure -> onFailure(sessionAuthenticateResult.exceptionOrNull() ?: Throwable("Session authenticate failed"))
                        sessionProposeResult.isFailure -> onFailure(sessionProposeResult.exceptionOrNull() ?: Throwable("Session proposal as a fallback failed"))
                        else -> onFailure(Throwable("Session authenticate failed, please try again"))
                    }
                }
            }
        }
    }

    private suspend fun isLinkModeEnabled(walletAppLink: String?) = !walletAppLink.isNullOrEmpty() && selfAppMetaData.redirect?.linkMode == true && linkModeStorageRepository.isEnabled(walletAppLink)

    private fun getSignReCapsJson(methods: List<String>?, authenticate: EngineDO.Authenticate) =
        if (!methods.isNullOrEmpty()) {
            val namespace = SignValidator.getNamespaceKeyFromChainId(authenticate.chains.first())
            val actionsJsonObject = JSONObject()
            methods.forEach { method -> actionsJsonObject.put("request/$method", JSONArray().put(0, JSONObject())) }
            JSONObject().put(ATT_KEY, JSONObject().put(namespace, actionsJsonObject)).toString().replace("\\/", "/")
        } else String.Empty

    private fun getExternalReCapsJson(authenticate: EngineDO.Authenticate): String = try {
        if (areExternalReCapsNotEmpty(authenticate)) {
            val externalUrn = authenticate.resources!!.last { resource -> resource.startsWith(RECAPS_PREFIX) }
            Base64.decode(externalUrn.removePrefix(RECAPS_PREFIX), Base64.NO_WRAP).toString(Charsets.UTF_8)
        } else {
            String.Empty
        }
    } catch (e: Exception) {
        String.Empty
    }

    private fun areExternalReCapsNotEmpty(authenticate: EngineDO.Authenticate): Boolean =
        authenticate.resources != null && authenticate.resources!!.any { resource -> resource.startsWith(RECAPS_PREFIX) }

    private suspend fun publishSessionAuthenticateDeferred(
        pairing: Core.Model.Pairing,
        authRequest: SignRpc.SessionAuthenticate,
        responseTopic: Topic,
        requestExpiry: Expiry
    ): Result<Unit> {
        logger.log("Sending session authenticate on topic: ${pairing.topic}")
        val irnParamsTtl = getIrnParamsTtl(requestExpiry, currentTimeInSeconds)
        val irnParams = IrnParams(Tags.SESSION_AUTHENTICATE, irnParamsTtl, true)
        val sessionAuthenticateDeferred = CompletableDeferred<Result<Unit>>()
        jsonRpcInteractor.publishJsonRpcRequest(Topic(pairing.topic), irnParams, authRequest,
            onSuccess = {
                logger.log("Session authenticate sent successfully on topic: ${pairing.topic}")
                sessionAuthenticateDeferred.complete(Result.success(Unit))
            },
            onFailure = { error ->
                jsonRpcInteractor.unsubscribe(responseTopic)
                logger.error("Failed to send a auth request: $error")
                sessionAuthenticateDeferred.complete(Result.failure(error))
            }
        )
        return sessionAuthenticateDeferred.await()
    }

    private suspend fun publishSessionProposeDeferred(
        pairing: Core.Model.Pairing,
        optionalNamespaces: Map<String, EngineDO.Namespace.Proposal>,
        responseTopic: Topic
    ): Result<Unit> {
        logger.log("Sending session proposal as a fallback on topic: ${pairing.topic}")
        val sessionProposeDeferred = CompletableDeferred<Result<Unit>>()
        proposeSessionUseCase.proposeSession(
            emptyMap(),
            optionalNamespaces,
            properties = null,
            pairing = pairing.toPairing(),
            onSuccess = {
                logger.log("Session proposal as a fallback sent successfully on topic: ${pairing.topic}")
                sessionProposeDeferred.complete(Result.success(Unit))
            },
            onFailure = { error ->
                jsonRpcInteractor.unsubscribe(responseTopic)
                logger.error("Failed to send a session proposal as a fallback: $error")
                sessionProposeDeferred.complete(Result.failure(error))
            }
        )
        return sessionProposeDeferred.await()
    }

    private fun getIrnParamsTtl(expiry: Expiry?, nowInSeconds: Long) = expiry?.run {
        val defaultTtl = dayInSeconds
        val extractedTtl = seconds - nowInSeconds
        val newTtl = extractedTtl.takeIf { extractedTtl >= defaultTtl } ?: defaultTtl
        Ttl(newTtl)
    } ?: Ttl(dayInSeconds)

    private fun generateUUID(): String = UUID.randomUUID().toString()
}

internal interface SessionAuthenticateUseCaseInterface {
    suspend fun authenticate(
        authenticate: EngineDO.Authenticate,
        methods: List<String>?,
        pairingTopic: String?,
        expiry: Expiry?,
        walletAppLink: String? = null,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit
    )
}