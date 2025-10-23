package app.perawallet.walletconnectv2.push.network

import app.perawallet.walletconnectv2.push.network.model.PushBody
import app.perawallet.walletconnectv2.push.network.model.PushResponse
import retrofit2.Response
import retrofit2.http.*

interface PushService {

    @POST("{projectId}/clients")
    suspend fun register(@Path("projectId") projectId: String, @Query("auth") clientID: String, @Body echoClientsBody: PushBody): Response<PushResponse>

    @DELETE("{projectId}/clients/{clientId}")
    suspend fun unregister(@Path("projectId") projectId: String, @Path("clientId") clientID: String): Response<PushResponse>
}