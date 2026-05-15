package lab3.egor.chat.data.repository

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lab3.egor.chat.data.model.*
import lab3.egor.chat.data.network.AuthInterceptor
import lab3.egor.chat.data.network.ChatApi
import lab3.egor.chat.data.storage.SettingsStorage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class ChatRepository(private val storage: SettingsStorage) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = false
    }

    private val _authErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authErrors: SharedFlow<Unit> = _authErrors.asSharedFlow()

    private val authInterceptor = AuthInterceptor()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor { message ->
            Log.d("CHAT_NETWORK", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://faerytea.name/")
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ChatApi::class.java)

    private suspend fun <T> checkAuth(response: Response<T>): T {
        if (response.code() == 401) {
            logout()
            _authErrors.emit(Unit)
            throw Exception("Unauthorized")
        }
        return response.body() ?: throw Exception("Empty response body")
    }

    suspend fun initToken() {
        val token = storage.token.firstOrNull()
        authInterceptor.setToken(token)
    }

    suspend fun getCurrentUser(): String = storage.name.firstOrNull() ?: ""

    suspend fun register(name: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.register(name)
            val body = response.body()?.string() ?: throw Exception("Empty body")
            val password = body.substringAfter("'").substringBefore("'")
            storage.saveCredentials(name, password)
            password
        }
    }

    suspend fun login(name: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.login(LoginRequest(name, pass))
            if (response.code() == 401) throw Exception("Invalid login or password")
            val token = response.body()?.string() ?: throw Exception("No token")
            storage.saveToken(token)
            storage.saveCredentials(name, pass)
            authInterceptor.setToken(token)
            token
        }
    }

    suspend fun getChannels(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching { 
            val response = api.getChannels()
            checkAuth(response)
        }
    }

    suspend fun getMessages(channel: String, lastId: String? = null): Result<List<Message>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getMessages(
                channelName = channel, 
                limit = 20, 
                reverse = true, 
                lastKnownId = lastId ?: "9999999999999"
            )
            checkAuth(response)
        }
    }

    suspend fun sendMessage(channel: String, text: String, from: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = SendMessageRequest(from, channel, MessageData(Text = TextContent(text.trim())))
            val response = api.sendMessage(request)
            checkAuth(response)
            Unit
        }
    }

    suspend fun sendImage(channel: String, from: String, bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val messageJson = json.encodeToString(
                SendMessageRequest.serializer(),
                SendMessageRequest(from, channel, MessageData(Image = ImageContent("")))
            )
            val msgPart = messageJson.toRequestBody("application/json".toMediaType())
            val imagePart = MultipartBody.Part.createFormData(
                "picture",
                "image.jpg",
                bytes.toRequestBody("image/jpeg".toMediaType())
            )
            val response = api.sendImage(msgPart, imagePart)
            checkAuth(response)
            Unit
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { api.logout() }
        storage.clear() // Очищаем токен для выхода
        authInterceptor.setToken(null)
    }
}
