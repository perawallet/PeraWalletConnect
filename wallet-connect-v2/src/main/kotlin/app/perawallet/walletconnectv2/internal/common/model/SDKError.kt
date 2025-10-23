package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent

class SDKError(val exception: Throwable) : EngineEvent