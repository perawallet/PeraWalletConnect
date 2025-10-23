package app.perawallet.walletconnectv2.test.client

import app.perawallet.walletconnectv2.BuildConfig
import app.perawallet.walletconnectv2.cacao.signature.SignatureType
import app.perawallet.walletconnectv2.internal.common.model.AccountId
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.test.activity.WCInstrumentedActivityScenario
import app.perawallet.walletconnectv2.test.utils.BNBAccount
import app.perawallet.walletconnectv2.test.utils.EthereumAccount
import app.perawallet.walletconnectv2.test.utils.SolanaAccount
import app.perawallet.walletconnectv2.test.utils.TestClient
import app.perawallet.walletconnectv2.test.utils.globalOnError
import app.perawallet.walletconnectv2.utils.cacao.CacaoSignerInterface
import app.perawallet.walletconnectv2.utils.cacao.sign
import app.perawallet.walletconnectv2.foundation.common.model.PrivateKey
import app.perawallet.walletconnectv2.foundation.util.jwt.encodeEd25519DidKey
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

class KeyserverInstrumentedAndroidTest {
    object CacaoSigner : CacaoSignerInterface<Cacao.Signature>

    @get:Rule
    val scenarioExtension = WCInstrumentedActivityScenario()

    private val statement = "dummyStatement"
    private val domain = "domain.dummy"

    @Test
    fun registerIdentityForEthereumByPrimaryClient() {
        Timber.d("registerIdentityByPrimaryClient: start")
        scenarioExtension.launch(BuildConfig.TEST_TIMEOUT_SECONDS.toLong()) {
            Timber.d(EthereumAccount.caip10)

            TestClient.Primary.identitiesInteractor.registerIdentity(
                AccountId(EthereumAccount.caip10),
                statement, domain, null, TestClient.Primary.keyserverUrl
            ) { message ->
                CacaoSigner.sign(message, PrivateKey(EthereumAccount.privKey).keyAsBytes, SignatureType.EIP191)
            }.fold(
                onSuccess = { registeredKey ->
                    Timber.d("Registered: ${encodeEd25519DidKey(registeredKey.keyAsBytes)}")

                    TestClient.Primary.identitiesInteractor.unregisterIdentity(
                        AccountId(EthereumAccount.caip10),
                        TestClient.Primary.keyserverUrl,
                    ).fold(
                        onSuccess = { unregisteredKey ->
                            Timber.d("Unregistered: ${encodeEd25519DidKey(unregisteredKey.keyAsBytes)}")
                            scenarioExtension.closeAsSuccess()
                        },
                        onFailure = ::globalOnError
                    )
                },
                onFailure = ::globalOnError
            )
        }
    }

    @Test
    fun failingRegisterIdentityForSolanaByPrimaryClient() {
        Timber.d("registerIdentityByPrimaryClient: start")
        scenarioExtension.launch(BuildConfig.TEST_TIMEOUT_SECONDS.toLong()) {
            Timber.d("${SolanaAccount.caip10}")
            TestClient.Primary.identitiesInteractor.registerIdentity(
                AccountId(SolanaAccount.caip10),
                statement, domain, emptyList(), TestClient.Primary.keyserverUrl
            ) { message ->
                CacaoSigner.sign(message, PrivateKey(SolanaAccount.privKey).keyAsBytes, SignatureType.EIP191)
            }.fold(
                onSuccess = { registeredKey ->
                    Timber.d("Registered: ${encodeEd25519DidKey(registeredKey.keyAsBytes)}")

                    TestClient.Primary.identitiesInteractor.unregisterIdentity(
                        AccountId(SolanaAccount.caip10),
                        TestClient.Primary.keyserverUrl,
                    ).fold(
                        onSuccess = { unregisteredKey ->
                            Timber.d("Unregistered: ${encodeEd25519DidKey(unregisteredKey.keyAsBytes)}")
                            // Note: When this error is shown that means Keys Server support solana, hence we need to update the tests here
                            globalOnError(Throwable("This test supposed to fail before reaching this part"))
                        },
                        // Note: When this error is shown that means Keys Server support solana, hence we need to update the tests here
                        onFailure = { globalOnError(Throwable("This test supposed to fail before reaching this part")) }
                    )
                },
                onFailure = { scenarioExtension.closeAsSuccess() }
            )
        }
    }

    @Test
    fun registerIdentityForBNBByPrimaryClient() {
        Timber.d("registerIdentityByPrimaryClient: start")
        scenarioExtension.launch(BuildConfig.TEST_TIMEOUT_SECONDS.toLong()) {
            TestClient.Primary.identitiesInteractor.registerIdentity(
                AccountId(BNBAccount.caip10),
                statement, domain, null, TestClient.Primary.keyserverUrl
            ) { message ->
                CacaoSigner.sign(message, PrivateKey(BNBAccount.privKey).keyAsBytes, SignatureType.EIP191)
            }.fold(
                onSuccess = { registeredKey ->
                    Timber.d("Registered: ${encodeEd25519DidKey(registeredKey.keyAsBytes)}")

                    TestClient.Primary.identitiesInteractor.unregisterIdentity(
                        AccountId(BNBAccount.caip10),
                        TestClient.Primary.keyserverUrl,
                    ).fold(
                        onSuccess = { unregisteredKey ->
                            Timber.d("Unregistered: ${encodeEd25519DidKey(unregisteredKey.keyAsBytes)}")
                            scenarioExtension.closeAsSuccess()
                        },
                        onFailure = ::globalOnError
                    )
                },
                onFailure = ::globalOnError
            )
        }
    }

}