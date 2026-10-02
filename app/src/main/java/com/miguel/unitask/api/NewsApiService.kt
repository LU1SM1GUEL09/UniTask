package com.miguel.unitask.api

import com.miguel.unitask.model.NewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Este es el contrato con el servidor. 
 * Aquí definimos qué "pedidos" podemos hacer.
 */
interface NewsApiService {

    /**
     * Esta función trae noticias sobre un tema (q) en un idioma específico.
     * Es 'suspend' porque tarda un poquito y no queremos que congele la app.
     */
    @GET("v2/everything")
    suspend fun getAcademicNews(
        @Query("q") query: String,
        @Query("language") language: String,
        @Query("pageSize") pageSize: Int,
        @Query("apiKey") apiKey: String
    ): Response<NewsResponse>
}
