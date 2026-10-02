package com.miguel.unitask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miguel.unitask.databinding.DialogAgregarNotaBinding
import com.miguel.unitask.databinding.FragmentNotasBinding

/**
 * Fragmento de Notas. Aquí es donde el estudiante lleva el control de sus calificaciones.
 * Calculamos el promedio automáticamente para que sepa cómo va en el semestre.
 */
class NotasFragment : Fragment() {
    private var _binding: FragmentNotasBinding? = null
    private val binding get() = _binding!!

    private val notas = mutableListOf<Nota>()
    private val materias = mutableListOf<Materia>()
    private lateinit var adapter: NotasAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Tipos de actividades que suelen calificar en la uni.
    private val tipos = listOf("Examen", "Tarea", "Participación", "Proyecto", "Laboratorio")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuramos la lista de notas.
        binding.notasRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotasAdapter(notas) { nota -> 
            // Al tocar una nota, sale el menú de editar/eliminar.
            mostrarOpcionesNota(nota) 
        }
        binding.notasRecycler.adapter = adapter

        // Botón (+) para subir una nueva calificación.
        binding.notasFab.setOnClickListener { mostrarDialogoNota() }

        // Gesto de refrescar.
        binding.notasSwipeRefresh.setOnRefreshListener {
            binding.notasSwipeRefresh.isRefreshing = false
        }

        cargarNotas()
        cargarMaterias()
    }

    /**
     * Traemos las materias para saber a qué asignatura le estamos poniendo la nota.
     */
    private fun cargarMaterias() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("materias")
            .addSnapshotListener { snapshots, _ ->
                materias.clear()
                snapshots?.forEach { doc ->
                    materias.add(Materia.fromMap(doc.id, doc.data))
                }
            }
    }

    /**
     * Obtenemos todas las notas de Firestore.
     */
    private fun cargarNotas() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("notas")
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                notas.clear()
                snapshots?.forEach { doc ->
                    notas.add(Nota.fromMap(doc.id, doc.data))
                }
                adapter.notifyDataSetChanged()
                
                // Si no hay notas, mostramos el mensaje de "Sin notas".
                val isEmpty = notas.isEmpty()
                binding.notasRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
                binding.notasEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                
                // Actualizamos el promedio de la pantalla.
                actualizarPromedio()
            }
    }

    /**
     * Lógica matemática para sacar el promedio ponderado.
     */
    private fun actualizarPromedio() {
        if (notas.isEmpty()) {
            binding.notasPromedio.text = "0.0"
            return
        }
        // Sumamos (Nota * Peso) y dividimos entre el peso total.
        val suma = notas.sumOf { it.calificacion * it.ponderacion }
        val pesos = notas.sumOf { it.ponderacion }
        val promedio = if (pesos > 0) suma / pesos else 0.0
        binding.notasPromedio.text = String.format("%.1f", promedio)
    }

    /**
     * Menú rápido para una nota específica.
     */
    private fun mostrarOpcionesNota(nota: Nota) {
        val options = arrayOf("Editar", "Eliminar")
        AlertDialog.Builder(requireContext())
            .setTitle("${nota.materiaNombre} - ${nota.tipo}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> mostrarDialogoNota(nota)
                    1 -> confirmarEliminarNota(nota)
                }
            }
            .show()
    }

    /**
     * Diálogo de seguridad para borrar notas.
     */
    private fun confirmarEliminarNota(nota: Nota) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar nota")
            .setMessage("¿Eliminar ${nota.tipo} de ${nota.materiaNombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                val uid = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(uid).collection("notas")
                    .document(nota.id).delete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Ventana (BottomSheet) para registrar o cambiar una nota.
     */
    private fun mostrarDialogoNota(nota: Nota? = null) {
        val isEditing = nota != null
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAgregarNotaBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.dialogNotaTitle.text = if (isEditing) "Editar Nota" else "Agregar Nota"

        // Si es edición, llenamos los campos con lo que ya sabemos.
        if (isEditing) {
            dialogBinding.dialogNotaMateria.setText(nota?.materiaNombre, false)
            dialogBinding.dialogNotaTipo.setText(nota?.tipo, false)
            dialogBinding.dialogNotaCalificacion.setText(nota?.calificacion.toString())
            dialogBinding.dialogNotaPorcentaje.setText((nota!!.ponderacion * 100).toInt().toString())
        }

        // Cargamos materias y tipos de actividad en los Spinners.
        val materiaNombres = materias.map { it.nombre }
        val materiaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, materiaNombres)
        dialogBinding.dialogNotaMateria.setAdapter(materiaAdapter)

        val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos)
        dialogBinding.dialogNotaTipo.setAdapter(tipoAdapter)

        dialogBinding.dialogNotaGuardar.setOnClickListener {
            val materiaText = dialogBinding.dialogNotaMateria.text?.toString()?.trim() ?: ""
            val tipoText = dialogBinding.dialogNotaTipo.text?.toString()?.trim() ?: ""
            val califText = dialogBinding.dialogNotaCalificacion.text?.toString()?.trim() ?: ""
            val porcentajeText = dialogBinding.dialogNotaPorcentaje.text?.toString()?.trim() ?: ""

            // Validaciones básicas para no subir basura a la base de datos.
            if (materiaText.isEmpty()) {
                dialogBinding.dialogNotaMateria.error = "Falta la materia"
                return@setOnClickListener
            }
            val calif = califText.toDoubleOrNull()
            if (calif == null || calif < 0 || calif > 100) {
                dialogBinding.dialogNotaCalificacion.error = "Debe ser de 0 a 100"
                return@setOnClickListener
            }

            val porcentaje = porcentajeText.toDoubleOrNull()
            if (porcentaje == null || porcentaje <= 0 || porcentaje > 100) {
                dialogBinding.dialogNotaPorcentaje.error = "Debe ser de 1 a 100"
                return@setOnClickListener
            }

            val materia = materias.find { it.nombre == materiaText }
            dialogBinding.dialogNotaGuardar.isEnabled = false

            // Armamos el objeto para Firestore.
            val notaData = mapOf(
                "materiaId" to (materia?.id ?: ""),
                "materiaNombre" to (materia?.nombre ?: materiaText),
                "tipo" to tipoText,
                "calificacion" to calif,
                "ponderacion" to (porcentaje / 100.0)
            )

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val ref = db.collection("users").document(uid).collection("notas")

            if (isEditing) {
                ref.document(nota!!.id).set(notaData)
                    .addOnSuccessListener { dialog.dismiss() }
                    .addOnFailureListener { dialogBinding.dialogNotaGuardar.isEnabled = true }
            } else {
                ref.add(notaData)
                    .addOnSuccessListener { dialog.dismiss() }
                    .addOnFailureListener { dialogBinding.dialogNotaGuardar.isEnabled = true }
            }
        }

        dialogBinding.dialogNotaCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
