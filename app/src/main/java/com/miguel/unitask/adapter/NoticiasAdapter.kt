package com.miguel.unitask.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.miguel.unitask.R
import com.miguel.unitask.databinding.ItemNoticiaBinding
import com.miguel.unitask.model.Article

/**
 * Adaptador para mostrar la lista de noticias en un RecyclerView.
 * @param articles Lista de artículos a mostrar.
 * @param onLeerMas Callback cuando se pulsa el botón "Leer más".
 * @param onFavorito Callback cuando se pulsa el botón "Favorito".
 */
class NoticiasAdapter(
    private val articles: List<Article>,
    private val onLeerMas: (Article) -> Unit,
    private val onFavorito: (Article) -> Unit
) : RecyclerView.Adapter<NoticiasAdapter.NoticiaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticiaViewHolder {
        val binding = ItemNoticiaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoticiaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoticiaViewHolder, position: Int) {
        holder.bind(articles[position])
    }

    override fun getItemCount(): Int = articles.size

    inner class NoticiaViewHolder(private val binding: ItemNoticiaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Enlaza los datos del artículo con la vista.
         */
        fun bind(article: Article) {
            binding.itemNoticiaTitulo.text = article.title ?: "Sin título"
            binding.itemNoticiaDesc.text = article.description ?: "Sin descripción disponible."
            binding.itemNoticiaFecha.text = article.publishedAt?.substring(0, 10) ?: ""

            // Cargar imagen con Glide
            Glide.with(binding.root.context)
                .load(article.urlToImage)
                .placeholder(R.drawable.stat_icon_bg_blue) // Placeholder temporal
                .error(R.drawable.stat_icon_bg_orange)    // Imagen de error
                .into(binding.itemNoticiaImagen)

            // Configurar clicks
            binding.itemNoticiaLeer.setOnClickListener { onLeerMas(article) }
            binding.itemNoticiaFav.setOnClickListener { onFavorito(article) }
        }
    }
}
