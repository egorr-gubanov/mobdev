package lab3.egor.chat.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    @Volatile
    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            token?.let { addHeader("X-Auth-Token", it) }
        }.build()
        return chain.proceed(request)
    }
}
