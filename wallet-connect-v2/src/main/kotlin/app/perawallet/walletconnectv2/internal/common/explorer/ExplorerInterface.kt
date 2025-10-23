package app.perawallet.walletconnectv2.internal.common.explorer

import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Project

interface ExplorerInterface {
    suspend fun getProjects(page: Int, entries: Int, isVerified: Boolean, isFeatured: Boolean): Result<List<Project>>
}