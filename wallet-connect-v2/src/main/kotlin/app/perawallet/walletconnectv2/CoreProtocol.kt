package app.perawallet.walletconnectv2

import android.app.Application
import android.content.SharedPreferences
import app.perawallet.walletconnectv2.di.coreStorageModule
import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.di.KEY_CLIENT_ID
import app.perawallet.walletconnectv2.internal.common.di.coreAndroidNetworkModule
import app.perawallet.walletconnectv2.internal.common.di.coreCommonModule
import app.perawallet.walletconnectv2.internal.common.di.coreCryptoModule
import app.perawallet.walletconnectv2.internal.common.di.coreJsonRpcModule
import app.perawallet.walletconnectv2.internal.common.di.corePairingModule
import app.perawallet.walletconnectv2.internal.common.di.explorerModule
import app.perawallet.walletconnectv2.internal.common.di.keyServerModule
import app.perawallet.walletconnectv2.internal.common.di.pulseModule
import app.perawallet.walletconnectv2.internal.common.di.pushModule
import app.perawallet.walletconnectv2.internal.common.di.web3ModalModule
import app.perawallet.walletconnectv2.internal.common.explorer.ExplorerInterface
import app.perawallet.walletconnectv2.internal.common.explorer.ExplorerProtocol
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.ProjectId
import app.perawallet.walletconnectv2.internal.common.model.Redirect
import app.perawallet.walletconnectv2.internal.common.model.TelemetryEnabled
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.pairing.client.PairingInterface
import app.perawallet.walletconnectv2.pairing.client.PairingProtocol
import app.perawallet.walletconnectv2.pairing.handler.PairingController
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.push.PushInterface
import app.perawallet.walletconnectv2.push.client.PushClient
import app.perawallet.walletconnectv2.relay.ConnectionType
import app.perawallet.walletconnectv2.relay.NetworkClientTimeout
import app.perawallet.walletconnectv2.relay.RelayClient
import app.perawallet.walletconnectv2.relay.RelayConnectionInterface
import app.perawallet.walletconnectv2.utils.isValidRelayServerUrl
import app.perawallet.walletconnectv2.utils.plantTimber
import app.perawallet.walletconnectv2.utils.projectId
import app.perawallet.walletconnectv2.verify.client.VerifyClient
import app.perawallet.walletconnectv2.verify.client.VerifyInterface
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module

class CoreProtocol(private val koinApp: KoinApplication = wcKoinApp) : CoreInterface {
    override val Pairing: PairingInterface = PairingProtocol(koinApp)
    override val PairingController: PairingControllerInterface = PairingController(koinApp)
    override var Relay = RelayClient(koinApp)

    override val Echo: PushInterface = PushClient
    override val Push: PushInterface = PushClient
    override val Verify: VerifyInterface = VerifyClient(koinApp)
    override val Explorer: ExplorerInterface = ExplorerProtocol(koinApp)

    init {
        plantTimber()
    }

    override fun setDelegate(delegate: CoreInterface.Delegate) {
        Pairing.setDelegate(delegate)
    }

    companion object {
        val instance = CoreProtocol()
    }

    override fun initialize(
        metaData: Core.Model.AppMetaData,
        relayServerUrl: String,
        connectionType: ConnectionType,
        application: Application,
        relay: RelayConnectionInterface?,
        keyServerUrl: String?,
        networkClientTimeout: NetworkClientTimeout?,
        telemetryEnabled: Boolean,
        onError: (Core.Model.Error) -> Unit
    ) {
        try {
            require(relayServerUrl.isValidRelayServerUrl()) { "Check the schema and projectId parameter of the Server Url" }

            setup(
                application = application,
                serverUrl = relayServerUrl,
                projectId = relayServerUrl.projectId(),
                telemetryEnabled = telemetryEnabled,
                connectionType = connectionType,
                networkClientTimeout = networkClientTimeout,
                relay = relay,
                onError = onError,
                metaData = metaData,
                keyServerUrl = keyServerUrl
            )
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
        }
    }

    override fun initialize(
        application: Application,
        projectId: String,
        metaData: Core.Model.AppMetaData,
        connectionType: ConnectionType,
        relay: RelayConnectionInterface?,
        keyServerUrl: String?,
        networkClientTimeout: NetworkClientTimeout?,
        telemetryEnabled: Boolean,
        onError: (Core.Model.Error) -> Unit
    ) {
        try {
            require(projectId.isNotEmpty()) { "Project Id cannot be empty" }

            setup(
                application = application,
                projectId = projectId,
                telemetryEnabled = telemetryEnabled,
                connectionType = connectionType,
                networkClientTimeout = networkClientTimeout,
                relay = relay,
                onError = onError,
                metaData = metaData,
                keyServerUrl = keyServerUrl
            )
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
        }
    }

    private fun CoreProtocol.setup(
        application: Application,
        serverUrl: String? = null,
        projectId: String,
        telemetryEnabled: Boolean,
        connectionType: ConnectionType,
        networkClientTimeout: NetworkClientTimeout?,
        relay: RelayConnectionInterface?,
        onError: (Core.Model.Error) -> Unit,
        metaData: Core.Model.AppMetaData,
        keyServerUrl: String?
    ) {
        val bundleId: String = application.packageName
        val relayServerUrl = if (serverUrl.isNullOrEmpty()) "wss://relay.walletconnect.org?projectId=$projectId" else serverUrl

        with(koinApp) {
            androidContext(application)
            modules(
                module { single { ProjectId(projectId) } },
                module { single(named(AndroidCommonDITags.TELEMETRY_ENABLED)) { TelemetryEnabled(telemetryEnabled) } },
                coreAndroidNetworkModule(relayServerUrl, connectionType, BuildConfig.SDK_VERSION, networkClientTimeout, bundleId),
                coreCommonModule(),
                coreCryptoModule(),
            )

            if (relay == null) {
                Relay.initialize(connectionType) { error -> onError(Core.Model.Error(error)) }
            }

            modules(
                coreStorageModule(bundleId = bundleId),
                module { single(named(AndroidCommonDITags.CLIENT_ID)) { requireNotNull(get<SharedPreferences>().getString(KEY_CLIENT_ID, null)) } },
                pushModule(),
                module { single { relay ?: Relay } },
                module {
                    single {
                        with(metaData) {
                            AppMetaData(
                                name = name,
                                description = description,
                                url = url,
                                icons = icons,
                                redirect = Redirect(native = redirect, universal = appLink, linkMode = linkMode)
                            )
                        }
                    }
                },
                module { single { Echo } },
                module { single { Push } },
                module { single { Verify } },
                coreJsonRpcModule(),
                corePairingModule(Pairing, PairingController),
                keyServerModule(keyServerUrl),
                explorerModule(),
                web3ModalModule(),
                pulseModule(bundleId)
            )
        }

        Pairing.initialize()
        PairingController.initialize()
        Verify.initialize()
    }
}