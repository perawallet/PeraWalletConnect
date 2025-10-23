package app.perawallet.walletconnectv1.impls

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import app.perawallet.walletconnectv1.EncryptedPayload
import app.perawallet.walletconnectv1.Session

class GsonPayloadAdapter(private val gson: Gson) : BasePayloadAdapter() {

    private val jsonRpcMapTypeToken = object : TypeToken<Map<String, Any?>?>() {}.type

    override fun parse(payload: String, key: String): Session.MethodCall {
        val encryptedPayload = gson.fromJson(payload, EncryptedPayload::class.java)
            ?: throw IllegalArgumentException("Invalid json payload!")

        val decryptedPayload = payloadEncryptionHelper.decryptPayload(encryptedPayload, key)

        val payloadJson = String(decryptedPayload)
        val jsonRpcMap: Map<String, Any>? = gson.fromJson(payloadJson, jsonRpcMapTypeToken)
        return parseJsonRpcMethodCall(jsonRpcMap)
    }

    override fun prepare(data: Session.MethodCall, key: String): String {
        val bytesData = gson.toJson(data.toJsonRpcMap()).toByteArray()
        val encryptedPayload = payloadEncryptionHelper.encryptPayload(bytesData, key)
        return gson.toJson(encryptedPayload)
    }
}
