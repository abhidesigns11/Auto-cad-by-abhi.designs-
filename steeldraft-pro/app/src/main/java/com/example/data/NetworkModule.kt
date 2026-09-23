package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Base URL for your own auth server (see /steeldraft-server). Configured via
 * API_BASE_URL in .env (Android project root) — defaults to the special
 * address the Android emulator uses to reach "localhost" on your computer,
 * so a fresh debug build works out of the box against a locally-running server.
 */
private fun resolveBaseUrl(): String {
    val url = BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() } ?: "http://10.0.2.2:4000/"
    return if (url.endsWith("/")) url else "$url/"
}

object NetworkModule {
    fun createApi(context: Context, tokenStore: TokenStore): SteelDraftApi {
        val authInterceptor = Interceptor { chain ->
            val token = tokenStore.getToken()
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        return Retrofit.Builder()
            .baseUrl(resolveBaseUrl())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SteelDraftApi::class.java)
    }
}
