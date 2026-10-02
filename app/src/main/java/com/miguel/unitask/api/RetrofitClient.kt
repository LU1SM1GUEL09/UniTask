package com.miguel.unitask.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * El Cliente de Retrofit es el que hace el trabajo sucio de conectarse a internet.
 * Lo hicimos 'Singleton' (object) para que no se creen mil conexiones a la vez.
 */
object RetrofitClient {
    private const val BASE_URL = "https://newsapi.org/"

    /**
     * Configuramos el servicio con un interceptor de logs.
     * Esto es muy útil para ver qué está pasando cuando falla algo.
     */
    val newsService: NewsApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Aquí le decimos que use Gson para traducir los JSON automáticamente
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(NewsApiService::class.java)
    }
}
