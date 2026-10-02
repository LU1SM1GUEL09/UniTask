package com.miguel.unitask

import android.app.DatePickerDialog
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
import com.miguel.unitask.databinding.DialogAgregarExamenBinding
import com.miguel.unitask.databinding.FragmentExamenesBinding
import java.util.Calendar

/**
 * Fragmento de Exámenes. Aquí el usuario puede ver sus fechas de parciales o finales.
 * Es vital para que no le agarren las prisas al estudiante.
 */
class ExamenesFragment : Fragment() {
    // Usamos View Binding para no pelearnos con el findViewById.
    private var _binding: FragmentExamenesBinding? = null
    private val binding get() = _binding!!

    private val examenes = mutableListOf<Examen>()
    private val materias = mutableListOf<Materia>()
    private lateinit var adapter: ExamenesAdapter
    
    // Conexión con Firestore para guardar todo en la nube.
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentExamenesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preparamos la lista para mostrar los exámenes.
        binding.examenesRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = ExamenesAdapter(examenes) { examen -> 
            // Si el usuario toca un examen, le damos opciones.
            mostrarOpcionesExamen(examen) 
        }
        binding.examenesRecycler.adapter = adapter

        // Botón flotante (+) para agregar un examen nuevo.
        binding.examenesFab.setOnClickListener { mostrarDialogoExamen() }

        // Gesto de jalar para actualizar (aunque Firestore ya lo hace en tiempo real).
        binding.examenesSwipeRefresh.setOnRefreshListener {
            binding.examenesSwipeRefresh.isRefreshing = false
        }

        cargarExamenes()
        cargarMaterias()
    }

    /**
     * Traemos las materias para que el usuario pueda elegir una del menú desplegable.
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
     * Obtenemos la lista de exámenes de Firestore. 
     * Usamos un SnapshotListener para que cualquier cambio se vea al instante.
     */
    private fun cargarExamenes() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("examenes")
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                examenes.clear()
                snapshots?.forEach { doc ->
                    examenes.add(Examen.fromMap(doc.id, doc.data))
                }
                adapter.notifyDataSetChanged()
                
                // Si no hay exámenes, mostramos un mensaje de "Lista vacía".
                val isEmpty = examenes.isEmpty()
                binding.examenesRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
                binding.examenesEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
    }

    /**
     * Menú que sale al tocar un examen: Editar o Eliminar.
     */
    private fun mostrarOpcionesExamen(examen: Examen) {
        val options = arrayOf("Editar", "Eliminar")
        AlertDialog.Builder(requireContext())
            .setTitle(examen.titulo)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> mostrarDialogoExamen(examen) // Abrimos el diálogo en modo edición.
                    1 -> confirmarEliminarExamen(examen)
                }
            }
            .show()
    }

    /**
     * Diálogo de seguridad para no borrar exámenes por error.
     */
    private fun confirmarEliminarExamen(examen: Examen) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar examen")
            .setMessage("¿Estás seguro de eliminar \"${examen.titulo}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                val uid = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(uid).collection("examenes")
                    .document(examen.id).delete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Ventana emergente (BottomSheet) para crear o editar un examen.
     */
    private fun mostrarDialogoExamen(examen: Examen? = null) {
        val isEditing = examen != null
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAgregarExamenBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.dialogExamenTitle.text = if (isEditing) "Editar Examen" else "Agregar Examen"

        // Si estamos editando, rellenamos los campos con la info guardada.
        if (isEditing) {
            dialogBinding.dialogExamenTitulo.setText(examen?.titulo)
            dialogBinding.dialogExamenMateria.setText(examen?.materiaNombre, false)
            dialogBinding.dialogExamenFecha.setText(examen?.fecha)
            dialogBinding.dialogExamenAula.setText(examen?.aula)
        }

        // Cargamos las materias en el dropdown.
        val materiaNombres = materias.map { it.nombre }
        val materiaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, materiaNombres)
        dialogBinding.dialogExamenMateria.setAdapter(materiaAdapter)

        // Selector de fecha nativo de Android.
        dialogBinding.dialogExamenFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    dialogBinding.dialogExamenFecha.setText(
                        String.format("%02d/%02d/%d", day, month + 1, year)
                    )
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Acción del botón guardar.
        dialogBinding.dialogExamenGuardar.setOnClickListener {
            val tituloText = dialogBinding.dialogExamenTitulo.text?.toString()?.trim() ?: ""
            val materiaText = dialogBinding.dialogExamenMateria.text?.toString()?.trim() ?: ""
            val fechaText = dialogBinding.dialogExamenFecha.text?.toString()?.trim() ?: ""
            val aulaText = dialogBinding.dialogExamenAula.text?.toString()?.trim() ?: ""

            if (tituloText.isEmpty()) {
                dialogBinding.dialogExamenTitulo.error = "Falta el título"
                return@setOnClickListener
            }

            val materia = materias.find { it.nombre == materiaText }
            dialogBinding.dialogExamenGuardar.isEnabled = false

            // Armamos el mapa de datos.
            val examenData = mapOf(
                "titulo" to tituloText,
                "materiaId" to (materia?.id ?: ""),
                "materiaNombre" to (materia?.nombre ?: materiaText),
                "fecha" to fechaText,
                "aula" to aulaText,
                "completado" to (examen?.completado ?: false)
            )

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val ref = db.collection("users").document(uid).collection("examenes")

            // Si es edición, sobreescribimos; si es nuevo, agregamos.
            if (isEditing) {
                ref.document(examen!!.id).set(examenData)
                    .addOnSuccessListener { dialog.dismiss() }
                    .addOnFailureListener { dialogBinding.dialogExamenGuardar.isEnabled = true }
            } else {
                ref.add(examenData)
                    .addOnSuccessListener { dialog.dismiss() }
                    .addOnFailureListener { dialogBinding.dialogExamenGuardar.isEnabled = true }
            }
        }

        dialogBinding.dialogExamenCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
