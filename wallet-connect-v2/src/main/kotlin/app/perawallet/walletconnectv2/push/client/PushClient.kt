@file:JvmSynthetic

package app.perawallet.walletconnectv2.push.client

import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.model.ProjectId
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.storage.push_messages.PushMessagesRepository
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.push.PushInterface
import app.perawallet.walletconnectv2.push.network.PushService
import app.perawallet.walletconnectv2.push.network.model.PushBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.qualifier.named

internal object PushClient : PushInterface {
    private val pushService by lazy { wcKoinApp.koin.get<PushService>() }
    override val clientId by lazy { wcKoinApp.koin.get<String>(named(AndroidCommonDITags.CLIENT_ID)) }
    private val projectId by lazy { wcKoinApp.koin.get<ProjectId>() }
    private val pushMessagesRepository: PushMessagesRepository by lazy { wcKoinApp.koin.get() }
    private const val SUCCESS_STATUS = "SUCCESS"

    override fun register(firebaseAccessToken: String, enableEncrypted: Boolean, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        if (enableEncrypted) pushMessagesRepository.enablePushNotifications()
        val body = PushBody(clientId, firebaseAccessToken, enableEncrypted = enableEncrypted)

        scope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val response = pushService.register(projectId.value, clientId, body)

                    if (response.isSuccessful && response.body() != null) {
                        if (response.body()!!.status == SUCCESS_STATUS) {
                            onSuccess()
                        } else {
                            onError(IllegalArgumentException(response.body()!!.errors?.first()?.message))
                        }
                    } else {
                        onError(IllegalArgumentException(response.errorBody()?.string()))
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }

    override fun unregister(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        scope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val response = pushService.unregister(projectId.value, clientId)

                    if (response.isSuccessful && response.body() != null) {
                        if (response.body()!!.status == SUCCESS_STATUS) {
                            onSuccess()
                        } else {
                            onError(IllegalArgumentException(response.body()!!.errors?.first()?.message))
                        }
                    } else {
                        onError(IllegalArgumentException(response.errorBody()?.string()))
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }
}