package app.perawallet.walletconnectv2.auth.engine

import app.perawallet.walletconnectv2.foundation.common.model.Topic

// idea: If we need responseTopic persistence throughout app terminations this is not sufficient
internal val pairingTopicToResponseTopicMap: MutableMap<Topic, Topic> = mutableMapOf()