package app.perawallet.walletconnectv2.internal.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.type.SerializableJsonRpc
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.ext.getFullName
import java.net.URI
import kotlin.reflect.KClass

@get:JvmSynthetic
val String.Companion.Empty
    get() = ""

@get:JvmSynthetic
val Int.Companion.DefaultId
    get() = -1

@JvmSynthetic
fun Long.extractTimestamp() = this / 1000

@JvmSynthetic
fun Expiry.isSequenceValid(): Boolean = seconds > currentTimeInSeconds

@get:JvmSynthetic
val String.Companion.HexPrefix
    get() = "0x"

fun <T : SerializableJsonRpc> Module.addSerializerEntry(value: KClass<T>): KoinDefinition<KClass<T>> =
    single(qualifier = named("key_${value.getFullName()}")) { value }

fun Module.addDeserializerEntry(key: String, value: KClass<*>): KoinDefinition<Pair<String, KClass<*>>> =
    single(qualifier = named("${key}_${value.getFullName()}")) { key to value }

// Had to add JsonAdapterEntry because Koin would fetch the wrong values when using Pair instead
data class JsonAdapterEntry<T>(val type: Class<T>, val adapter: (Moshi) -> JsonAdapter<T>) {
    override fun toString(): String = "JsonAdapterEntry(type=${type.name})"
}

fun <T> Module.addJsonAdapter(type: Class<T>, adapter: (Moshi) -> JsonAdapter<T>): KoinDefinition<JsonAdapterEntry<T>> {
    val jsonAdapterEntry = JsonAdapterEntry(type, adapter)
    return single(qualifier = named("$jsonAdapterEntry")) { jsonAdapterEntry }
}

@JvmSynthetic
fun compareDomains(metadataUrl: String, originUrl: String): Boolean {
    try {
        val metadataDomain = URI(metadataUrl).host.removePrefix("www.")
        val originDomain = URI(originUrl).host.removePrefix("www.")
        return metadataDomain == originDomain
    } catch (e: Exception) {
        return false
    }
}