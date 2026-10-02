package com.miguel.unitask

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.miguel.unitask.databinding.ActivityMainBinding

/**
 * MainActivity: Es la ventana principal de la app. 
 * Su única chamba es ser el contenedor de todos los fragmentos usando Navigation Component.
 */
class MainActivity : AppCompatActivity() {
    // Usamos binding para acceder al contenedor principal ('main') fácilmente.
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Habilitamos el diseño de borde a borde para que la app aproveche toda la pantalla.
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Ajustamos los márgenes para que los elementos no se tapen con la cámara o la barra de batería.
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
