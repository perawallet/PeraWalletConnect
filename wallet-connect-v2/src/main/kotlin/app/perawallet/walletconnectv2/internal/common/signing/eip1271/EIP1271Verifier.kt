package app.perawallet.walletconnectv2.internal.common.signing.eip1271

import app.perawallet.walletconnectv2.foundation.util.bytesToHex
import app.perawallet.walletconnectv2.foundation.util.generateId
import app.perawallet.walletconnectv2.internal.common.signing.signature.Signature
import app.perawallet.walletconnectv2.internal.common.signing.signature.toCacaoSignature
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import timber.log.Timber

internal object EIP1271Verifier {
    private const val isValidSignatureHash = "0x1626ba7e"
    private const val method = "eth_call"
    private const val dynamicTypeOffset =
        "0000000000000000000000000000000000000000000000000000000000000040"
    private const val dynamicTypeLength =
        "0000000000000000000000000000000000000000000000000000000000000041"
    private const val mediaTypeString = "application/json; charset=utf-8"
    private const val rpcUrlPrefix = "https://rpc.walletconnect.com/v1/?chainId=eip155:1&projectId="
    private const val hexPrefix = "0x"

    private fun getValidResponse(id: Long): String =
        "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":\"0x1626ba7e00000000000000000000000000000000000000000000000000000000\"}"

    private fun String.prefixWithRpcUrl(): String = rpcUrlPrefix + this

    private fun createBody(to: String, data: String, id: Long): RequestBody {
        val jsonMediaType: MediaType = mediaTypeString.toMediaType()
        val postBody = """{
                |"method" : "$method",
                |"params" : [{"to":"$to", "data":"$data"}, "latest"],
                |"id":${id}, "jsonrpc":"2.0"
                |}""".trimMargin()

        return postBody.toRequestBody(jsonMediaType)
    }

    fun verify(
        signature: Signature,
        originalMessage: String,
        address: String,
        projectId: String
    ): Boolean {
        return try {
            val messageHash: String =
                getEthereumMessageHash(originalMessage.toByteArray()).bytesToHex()
            verify(messageHash, signature, projectId, address)
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    private fun getEthereumMessageHash(message: ByteArray): ByteArray {
        val prefix = "\u0019Ethereum Signed Message:\n${message.size}".toByteArray()
        val toHash = ByteArray(prefix.size + message.size).apply {
            System.arraycopy(prefix, 0, this, 0, prefix.size)
            System.arraycopy(message, 0, this, prefix.size, message.size)
        }
        return Hash.sha3(toHash)
    }

    fun verifyHex(
        signature: Signature,
        hexMessage: String,
        address: String,
        projectId: String
    ): Boolean {
        return try {
            val messageHash: String =
                getEthereumMessageHash(Numeric.hexStringToByteArray(hexMessage)).bytesToHex()
            verify(messageHash, signature, projectId, address)
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    private fun verify(
        messageHash: String,
        signature: Signature,
        projectId: String,
        address: String
    ): Boolean {
        val data: String =
            isValidSignatureHash + messageHash + dynamicTypeOffset + dynamicTypeLength + signature.toCacaoSignature()
                .removePrefix(hexPrefix)

        val id = generateId()
        val request: Request =
            Request.Builder().url(projectId.prefixWithRpcUrl()).post(createBody(address, data, id))
                .build()
        val response: Response = OkHttpClient().newCall(request).execute()
        val responseString = response.body?.string() ?: throw Exception("Response body is null")
        val validResponseResult = getResponseResult(getValidResponse(id))
        val responseResult = getResponseResult(responseString)

        return responseResult == validResponseResult
    }

    private fun getResponseResult(payload: String): String {
        return JSONObject(payload.trimIndent()).get("result") as String
    }
}