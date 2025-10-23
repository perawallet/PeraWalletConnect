package app.perawallet.walletconnectv2.verify.data

import app.perawallet.walletconnectv2.verify.model.Origin
import app.perawallet.walletconnectv2.verify.model.RegisterAttestationBody
import app.perawallet.walletconnectv2.verify.model.VerifyServerPublicKey
import app.perawallet.walletconnectv2.verify.model.VerifyServerResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface VerifyService {
    @Headers("Content-Type: application/json")
    @POST("attestation")
    suspend fun registerAttestation(@Body body: RegisterAttestationBody): Response<VerifyServerResponse.RegisterAttestation>

    @Headers("Content-Type: application/json")
    @GET("attestation/{attestationId}?v2Supported=true")
    suspend fun resolveAttestation(@Path("attestationId") attestationId: String): Response<Origin>

    @GET("v2/public-key")
    suspend fun getPublicKey(): Response<VerifyServerPublicKey>
}