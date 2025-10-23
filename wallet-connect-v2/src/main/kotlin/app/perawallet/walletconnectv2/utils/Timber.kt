package app.perawallet.walletconnectv2.utils

import app.perawallet.walletconnectv2.BuildConfig
import timber.log.Timber

internal fun plantTimber() {
    if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "internal") {
        if (Timber.treeCount == 0) {
            Timber.plant(
                object : Timber.DebugTree() {
                    /**
                     * Override [log] to modify the tag and add a "global tag" prefix to it. You can rename the String "global_tag_" as you see fit.
                     */
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        super.log(priority, "WalletConnectV2", message, t)
                    }
                }
            )
        }
    }
}