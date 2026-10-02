package com.miguel.unitask.repository

import com.miguel.unitask.api.RetrofitClient
import com.miguel.unitask.model.NewsResponse
import retrofit2.Response

/**
 * El Repositorio es como el puente entre el servicio de internet y nuestra Activity.
 * Su chamba es nada más pedir las noticias y devolverlas tal cual.
 */
class NewsRepository {

    /**
     * Llama al servicio de noticias académicas.
     * Le pasamos los filtros de una vez para que la Activity no se preocupe por eso.
     */
    suspend fun getAcademicNews(apiKey: String): Response<NewsResponse> {
        return RetrofitClient.newsService.getAcademicNews(
            query = "educacion",
            language = "es",
            pageSize = 20,
            apiKey = apiKey
        )
    }
}
