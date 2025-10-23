@file:Suppress("PackageDirectoryMismatch")

package app.perawallet.walletconnectv2.cacao

interface SignatureInterface {
    val t: String
    val s: String
    val m: String?
}