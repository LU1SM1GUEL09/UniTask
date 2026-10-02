package com.miguel.unitask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miguel.unitask.databinding.FragmentPerfilBinding

/**
 * Fragmento de Perfil. Aquí el usuario puede ver su carrera, semestre
 * y cambiar su nombre si quiere.
 */
class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cargamos los datos guardados en la nube
        cargarPerfil()

        binding.perfilEditar.setOnClickListener { mostrarDialogoEditar() }
        
        // El botón de cerrar sesión. Reiniciamos la app para que sea un proceso limpio.
        binding.perfilCerrarSesion.setOnClickListener {
            auth.signOut()
            val intent = android.content.Intent(requireContext(), MainActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    /**
     * Traemos la información del estudiante de Firestore.
     * Si no hay internet, la app mostrará lo último que guardó en caché.
     */
    private fun cargarPerfil() {
        val user = auth.currentUser
        // Valores por defecto de Auth
        binding.perfilNombre.text = user?.displayName ?: "Usuario"
        binding.perfilEmail.text = user?.email ?: ""
        binding.perfilNombreCompleto.text = user?.displayName ?: "--"

        val uid = user?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                // Revisamos si el binding sigue vivo para evitar que la app truene al cerrar sesión
                if (_binding == null) return@addOnSuccessListener
                
                // Si guardamos un nombre diferente en la base de datos, lo usamos
                val nombreFirestore = doc.getString("nombre")
                if (!nombreFirestore.isNullOrEmpty()) {
                    binding.perfilNombre.text = nombreFirestore
                    binding.perfilNombreCompleto.text = nombreFirestore
                }

                binding.perfilCarrera.text = doc.getString("carrera") ?: "--"
                binding.perfilSemestre.text = doc.getString("semestre") ?: "--"
            }
    }

    /**
     * Abrimos un diálogo para que el usuario pueda actualizar su info.
     * Ahora incluimos Nombre, Carrera y Semestre para que el perfil esté completo.
     */
    private fun mostrarDialogoEditar() {
        val context = requireContext()
        
        // Creamos un contenedor vertical para meter los tres campos de texto
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val padding = 24.dpToPx()
            setPadding(padding, 16.dpToPx(), padding, 0)
        }

        // Función de ayuda para crear cada campo rápido y sin repetir tanto código
        fun crearCampo(hintTexto: String, valorInicial: String): TextInputEditText {
            val layout = TextInputLayout(context).apply {
                hint = hintTexto
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 16.dpToPx()
                layoutParams = lp
            }
            val input = TextInputEditText(context).apply {
                setText(if (valorInicial == "--") "" else valorInicial)
            }
            layout.addView(input)
            container.addView(layout)
            return input
        }

        // Creamos los tres campos con la info que ya tenemos en pantalla
        val inputNombre = crearCampo("Nombre completo", binding.perfilNombreCompleto.text.toString())
        val inputCarrera = crearCampo("Carrera", binding.perfilCarrera.text.toString())
        val inputSemestre = crearCampo("Semestre", binding.perfilSemestre.text.toString())

        MaterialAlertDialogBuilder(context)
            .setTitle("Editar Perfil")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = inputNombre.text?.toString()?.trim() ?: ""
                val nuevaCarrera = inputCarrera.text?.toString()?.trim() ?: ""
                val nuevoSemestre = inputSemestre.text?.toString()?.trim() ?: ""

                val uid = auth.currentUser?.uid ?: return@setPositiveButton

                // 1. Actualizamos el nombre en Firebase Auth (el de la cuenta principal)
                if (nuevoNombre.isNotEmpty()) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(nuevoNombre)
                        .build()
                    auth.currentUser?.updateProfile(profileUpdates)
                }

                // 2. Guardamos todo en Firestore para que no se pierda nada
                val datos = mapOf(
                    "nombre" to nuevoNombre,
                    "carrera" to nuevaCarrera,
                    "semestre" to nuevoSemestre
                )

                db.collection("users").document(uid).set(datos, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        if (_binding == null) return@addOnSuccessListener
                        // Actualizamos la pantalla de inmediato para que el usuario vea sus cambios
                        binding.perfilNombre.text = nuevoNombre.ifEmpty { "Usuario" }
                        binding.perfilNombreCompleto.text = nuevoNombre.ifEmpty { "--" }
                        binding.perfilCarrera.text = nuevaCarrera.ifEmpty { "--" }
                        binding.perfilSemestre.text = nuevoSemestre.ifEmpty { "--" }
                        Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Carpintería de diseño: convierte números a densidad de pantalla
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
