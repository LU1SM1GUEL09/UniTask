package com.miguel.unitask

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miguel.unitask.adapter.NoticiasAdapter
import com.miguel.unitask.databinding.ActivityNoticiasBinding
import com.miguel.unitask.model.Article
import com.miguel.unitask.repository.NewsRepository
import kotlinx.coroutines.launch
import java.net.UnknownHostException

/**
 * Esta es la pantalla donde cargamos las noticias académicas.
 */
class NoticiasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoticiasBinding
    
    // El repositorio es como nuestro bibliotecario personal, él sabe ir a buscar las noticias.
    private val repository = NewsRepository()
    
    // Necesitamos Firebase para que el usuario guarde lo que le guste.
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Mi clave de NewsAPI. Sin esto, la nube no nos deja pasar.
    private val apiKey = "685a77936bf14217b2912f0b0836baee"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticiasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        // Mandamos a buscar las noticias nada más abrir la pantalla.
        fetchNews()
    }

    /**
     * Ponemos la flechita de atrás para que el usuario no se pierda.
     */
    private fun setupToolbar() {
        binding.noticiasToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Aquí hacemos la llamada al servidor.
     */
    private fun fetchNews() {
        // Ponemos a dar vueltas el circulito de carga.
        binding.noticiasLoader.visibility = View.VISIBLE
        binding.noticiasEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Hacemos el puente con el repositorio.
                val response = repository.getAcademicNews(apiKey)
                binding.noticiasLoader.visibility = View.GONE

                if (response.isSuccessful && (response.body() != null)) {
                    val articles = response.body()!!.articles
                    if (articles.isNotEmpty()) {
                        // Si todo salió bien, dibujamos la lista.
                        setupRecyclerView(articles)
                    } else {
                        showEmptyState()
                    }
                } else {
                    // Si el servidor nos dice que algo anda mal.
                    showEmptyState()
                }
            } catch (e: Exception) {
                binding.noticiasLoader.visibility = View.GONE
                // Manejamos el error si no hay internet (UnknownHostException).
                if (e is UnknownHostException) {
                    showEmptyState()
                } else {
                    Toast.makeText(this@NoticiasActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Configuramos el RecyclerView para mostrar cada noticia en una tarjeta.
     */
    private fun setupRecyclerView(articles: List<Article>) {
        val adapter = NoticiasAdapter(
            articles = articles,
            onLeerMas = { article ->
                // Esto abre el link de la noticia en Google Chrome o el navegador del usuario.
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                startActivity(intent)
            }
        ) { article ->
            // Mandamos a guardar los datos de la noticia a Firebase.
            guardarEnFavoritos(article)
        }
        binding.noticiasRecycler.layoutManager = LinearLayoutManager(this)
        binding.noticiasRecycler.adapter = adapter
    }

    /**
     * Guardamos la noticia en una colección de "Favoritos" en Firestore. 
     * Así el estudiante puede leerla después aunque no tenga internet.
     */
    private fun guardarEnFavoritos(article: Article) {
        val uid = auth.currentUser?.uid ?: return
        
        // Creamos el objeto que se va a subir a la nube.
        val favorito = hashMapOf(
            "uid" to uid,
            "titulo" to (article.title ?: "Sin título"),
            "descripcion" to (article.description ?: ""),
            "imagen" to (article.urlToImage ?: ""),
            "fecha" to (article.publishedAt ?: ""),
            "url" to (article.url ?: "")
        )

        db.collection("Favoritos")
            .add(favorito)
            .addOnSuccessListener {
                Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Mostramos el mensaje amigable de "No fue posible obtener las noticias" si algo falla.
     */
    private fun showEmptyState() {
        binding.noticiasEmpty.visibility = View.VISIBLE
    }
}
