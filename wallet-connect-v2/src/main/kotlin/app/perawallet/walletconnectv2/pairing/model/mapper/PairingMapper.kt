package app.perawallet.walletconnectv2.pairing.model.mapper

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.Pairing
import app.perawallet.walletconnectv2.internal.common.model.Redirect
import app.perawallet.walletconnectv2.pairing.engine.model.EngineDO
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.internal.utils.Empty


internal fun EngineDO.PairingDelete.toCore(): Core.Model.DeletedPairing =
    Core.Model.DeletedPairing(topic, reason)


internal fun Pairing.toCore(): Core.Model.Pairing =
    Core.Model.Pairing(
        topic.value,
        expiry.seconds,
        peerAppMetaData?.toCore(),
        relayProtocol,
        relayData,
        uri,
        isActive = true,
        methods ?: String.Empty
    )


fun Core.Model.Pairing.toPairing(): Pairing =
    Pairing(
        Topic(topic),
        Expiry(expiry),
        peerAppMetaData?.toAppMetaData(),
        relayProtocol,
        relayData,
        uri,
        methods = registeredMethods
    )


internal fun Core.Model.AppMetaData.toAppMetaData() = AppMetaData(name = name, description = description, url = url, icons = icons, redirect = Redirect(redirect))


internal fun AppMetaData?.toCore() = Core.Model.AppMetaData(this?.name ?: String.Empty, this?.description ?: String.Empty, this?.url ?: String.Empty, this?.icons ?: emptyList(), this?.redirect?.native)