package lab3.egor.chat.data.network

import lab3.egor.chat.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {

    @FormUrlEncoded
    @POST("addusr")
    suspend fun register(@Field("name") name: String): Response<ResponseBody>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>

    @GET("channels")
    suspend fun getChannels(): Response<List<String>>

    @GET("channel/{name}")
    suspend fun getMessages(
        @Path("name") channelName: String,
        @Query("limit") limit: Int? = null,
        @Query("lastKnownId") lastKnownId: String? = null,
        @Query("reverse") reverse: Boolean? = null
    ): Response<List<Message>>

    @POST("messages")
    suspend fun sendMessage(@Body message: SendMessageRequest): Response<ResponseBody>

    @Multipart
    @POST("messages")
    suspend fun sendImage(
        @Part("msg") message: RequestBody,
        @Part picture: MultipartBody.Part
    ): Response<ResponseBody>

    @POST("logout")
    suspend fun logout(): Response<Unit>
}
