@file:JvmSynthetic

package app.perawallet.walletconnectv2.web3.wallet.utils

import app.perawallet.walletconnectv2.cacao.signature.ISignatureType
import app.perawallet.walletconnectv2.utils.cacao.CacaoSignerInterface
import app.perawallet.walletconnectv2.web3.wallet.client.Wallet


/// Only added to have backwards compatibility. Newer SDKs should only add CacaoSigner object below.
enum class SignatureType(override val header: String) : ISignatureType {
    EIP191("eip191"), EIP1271("eip1271");
}


object CacaoSigner : CacaoSignerInterface<Wallet.Model.Cacao.Signature>