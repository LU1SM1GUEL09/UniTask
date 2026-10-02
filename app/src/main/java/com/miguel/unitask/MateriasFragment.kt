package com.miguel.unitask

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miguel.unitask.databinding.DialogAgregarMateriaBinding
import com.miguel.unitask.databinding.FragmentMateriasBinding

/**
 * Fragmento de Materias. Es el primer paso para organizar el semestre.
 * Aquí el usuario registra sus asignaturas y les asigna un color.
 */
class MateriasFragment : Fragment() {
    // Usamos View Binding para no pelearnos con el findViewById y que el código sea más limpio
    private var _binding: FragmentMateriasBinding? = null
    private val binding get() = _binding!!

    private val materias = mutableListOf<Materia>()
    private lateinit var adapter: MateriasAdapter
    private var selectedColor = 0

    // Instancias de Firebase para jalar los datos de la nube
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Paleta de colores predefinida para que la app se vea armónica.
    private val colores = listOf(
        0xFF6750A4.toInt(), 0xFFE91E63.toInt(), 0xFFFF5722.toInt(), 0xFFFF9800.toInt(),
        0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFF00BCD4.toInt(), 0xFF607D8B.toInt()
    )

    /**
     * Pequeño menú que sale al mantener presionada una materia para borrarla.
     */
    private fun mostrarOpcionesMateria(materia: Materia) {
        val options = arrayOf("Eliminar")
        AlertDialog.Builder(requireContext())
            .setTitle(materia.nombre)
            .setItems(options) { _, which ->
                if (which == 0) confirmarEliminarMateria(materia)
            }
            .show()
    }

    /**
     * Diálogo de seguridad para no borrar materias por error.
     */
    private fun confirmarEliminarMateria(materia: Materia) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar materia")
            .setMessage("¿Estás seguro de eliminar \"${materia.nombre}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                val uid = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(uid).collection("materias")
                    .document(materia.id).delete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMateriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuramos el listado de materias.
        binding.materiasRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = MateriasAdapter(materias,
            onClick = { materia ->
                // Al tocar una materia, vamos a su pantalla de detalle.
                val bundle = Bundle().apply { putString("materiaId", materia.id) }
                findNavController().navigate(R.id.action_materias_to_detalle_mat, bundle)
            },
            onLongClick = { materia -> mostrarOpcionesMateria(materia) }
        )
        binding.materiasRecycler.adapter = adapter

        // Botón flotante para registrar una nueva materia.
        binding.materiasFab.setOnClickListener { mostrarDialogoAgregar() }

        // Gesto de jalar para actualizar.
        binding.materiasSwipeRefresh.setOnRefreshListener {
            binding.materiasSwipeRefresh.isRefreshing = false
        }

        cargarMaterias()
    }

    /**
     * Traemos la lista de materias de la colección 'materias' en Firestore.
     */
    private fun cargarMaterias() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("materias")
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                materias.clear()
                snapshots?.forEach { doc ->
                    materias.add(Materia.fromMap(doc.id, doc.data))
                }
                adapter.notifyDataSetChanged()
                
                // Si la lista está vacía, mostramos el aviso de "Aún no tienes materias".
                val isEmpty = materias.isEmpty()
                binding.materiasRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
                binding.materiasEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
    }

    /**
     * Ventana emergente para crear materias. 
     * Incluimos un selector de color dinámico hecho con ImageViews.
     */
    private fun mostrarDialogoAgregar() {
        selectedColor = colores[0]
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAgregarMateriaBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Generamos los círculos de colores dinámicamente en el GridLayout.
        colores.forEachIndexed { index, color ->
            val imageView = ImageView(requireContext()).apply {
                val bgShape = ContextCompat.getDrawable(requireContext(), R.drawable.color_circle_unselected)?.mutate()
                (bgShape as? GradientDrawable)?.setColor(color)
                setImageDrawable(bgShape)
                setPadding(8, 8, 8, 8)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dpToPx(48); height = dpToPx(48)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                }
                setOnClickListener {
                    // Al seleccionar un color, actualizamos el borde visual.
                    selectedColor = color
                    for (i in 0 until dialogBinding.dialogMateriaColores.childCount) {
                        val child = dialogBinding.dialogMateriaColores.getChildAt(i) as? ImageView
                        val bg = child?.drawable as? GradientDrawable
                        if (i == index) {
                            child?.setImageResource(R.drawable.color_circle_selected)
                            child?.setPadding(8, 8, 8, 8)
                            bg?.setColor(color.toInt())
                        } else {
                            child?.setImageResource(R.drawable.color_circle_unselected)
                            child?.setPadding(4, 4, 4, 4)
                            bg?.setColor(colores[i])
                        }
                    }
                }
                contentDescription = "Color $index"
            }
            if (index == 0) {
                val bgShape = ContextCompat.getDrawable(requireContext(), R.drawable.color_circle_selected)?.mutate()
                (bgShape as? GradientDrawable)?.setColor(color)
                imageView.setImageDrawable(bgShape)
                imageView.setPadding(8, 8, 8, 8)
            }
            dialogBinding.dialogMateriaColores.addView(imageView)
        }

        // Acción de guardar la materia en la nube.
        dialogBinding.dialogMateriaGuardar.setOnClickListener {
            val nombreText = dialogBinding.dialogMateriaNombre.text?.toString()?.trim() ?: ""
            val profesorText = dialogBinding.dialogMateriaProfesor.text?.toString()?.trim() ?: ""
            val infoText = dialogBinding.dialogMateriaInfo.text?.toString()?.trim() ?: ""

            if (nombreText.isEmpty()) {
                dialogBinding.dialogMateriaNombre.error = "Escribe el nombre"
                return@setOnClickListener
            }

            dialogBinding.dialogMateriaGuardar.isEnabled = false

            val materia = Materia(
                nombre = nombreText,
                profesor = profesorText.ifEmpty { "Sin profesor" },
                info = infoText,
                color = selectedColor
            )

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            db.collection("users").document(uid).collection("materias")
                .add(materia.toMap())
                .addOnSuccessListener {
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    dialogBinding.dialogMateriaGuardar.isEnabled = true
                }
        }

        dialogBinding.dialogMateriaCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // Una función de ayuda para convertir DP a Píxeles y que el diseño no se rompa
    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
