package app.perawallet.walletconnectv2.sign.engine

import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import java.util.concurrent.ConcurrentLinkedQueue

internal val sessionRequestEventsQueue = ConcurrentLinkedQueue<EngineDO.SessionRequestEvent>()