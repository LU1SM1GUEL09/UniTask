package com.miguel.unitask.model

import com.google.gson.annotations.SerializedName

/**
 * Estos son los moldes de las noticias. 
 * El JSON que viene de internet es un texto largo; estas clases nos ayudan a 
 * convertir ese texto en objetos que Kotlin pueda usar fácilmente.
 */
data class NewsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("totalResults") val totalResults: Int,
    @SerializedName("articles") val articles: List<Article>
)

/**
 * Representa una noticia individual. 
 * Le pusimos "?" a los tipos para que la app no truene si falta algún dato.
 */
data class Article(
    @SerializedName("source") val source: Source?,
    @SerializedName("author") val author: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("content") val content: String?
)

/**
 * Modelo que representa la fuente de la noticia.
 */
data class Source(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)
