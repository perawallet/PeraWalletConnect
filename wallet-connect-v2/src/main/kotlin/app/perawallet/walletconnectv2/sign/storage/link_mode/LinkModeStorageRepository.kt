package app.perawallet.walletconnectv2.sign.storage.link_mode

import android.database.sqlite.SQLiteException
import app.perawallet.walletconnectv2.sign.storage.`data`.dao.linkmode.LinkModeDaoQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


internal class LinkModeStorageRepository(
    private val linkModeDaoQueries: LinkModeDaoQueries
) {
    
    @Throws(SQLiteException::class)
    suspend fun insert(appLink: String) = withContext(Dispatchers.IO) {
        linkModeDaoQueries.insertOrIgnore(appLink)
    }

    
    @Throws(SQLiteException::class)
    suspend fun isEnabled(appLink: String): Boolean = withContext(Dispatchers.IO) {
        linkModeDaoQueries.isEnabled(appLink).executeAsOneOrNull() != null
    }
}