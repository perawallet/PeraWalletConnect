package app.perawallet.walletconnectv2.sign.storage.link_mode

import android.database.sqlite.SQLiteException
import app.perawallet.walletconnectv2.sign.storage.`data`.dao.linkmode.LinkModeDaoQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


internal class LinkModeStorageRepository(
    private val linkModeDaoQueries: LinkModeDaoQueries
) {
    @JvmSynthetic
    @Throws(SQLiteException::class)
    suspend fun insert(appLink: String) = withContext(Dispatchers.IO) {
        linkModeDaoQueries.insertOrIgnore(appLink)
    }

    @JvmSynthetic
    @Throws(SQLiteException::class)
    suspend fun isEnabled(appLink: String): Boolean = withContext(Dispatchers.IO) {
        linkModeDaoQueries.isEnabled(appLink).executeAsOneOrNull() != null
    }
}