package com.miguel.unitask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miguel.unitask.databinding.FragmentDetalleMatBinding

/**
 * Fragmento de Detalle de Materia. Aquí es donde el estudiante ve a fondo la info
 * de una materia específica, como el nombre del profesor y notas adicionales.
 */
class DetalleMatFragment : Fragment() {
    // Usamos View Binding para acceder a los TextViews fácilmente.
    private var _binding: FragmentDetalleMatBinding? = null
    private val binding get() = _binding!!

    // Firebase para jalar la info de la materia seleccionada.
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDetalleMatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recuperamos el ID de la materia que nos pasaron desde la pantalla anterior.
        val materiaId = arguments?.getString("materiaId") ?: return
        cargarMateria(materiaId)
    }

    /**
     * Busca los datos de la materia en Firestore usando su ID.
     */
    private fun cargarMateria(materiaId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("materias")
            .document(materiaId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                // Convertimos el documento de Firebase en un objeto Materia de Kotlin.
                val materia = Materia.fromMap(doc.id, doc.data ?: emptyMap())
                
                // Pintamos la información en la pantalla.
                binding.detalleMatNombre.text = materia.nombre
                binding.detalleMatProfesor.text = materia.profesor
                binding.detalleMatHorario.text = "Info: ${materia.info.ifEmpty { "Sin información" }}"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
