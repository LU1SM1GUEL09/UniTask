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
import com.miguel.unitask.databinding.DialogAgregarTareaBinding
import com.miguel.unitask.databinding.FragmentTareasBinding
import java.util.Calendar

/**
 * Fragmento de Tareas. Aquí es donde el estudiante organiza su día a día.
 * Lo diseñamos para que sea muy visual y fácil de filtrar.
 */
class TareasFragment : Fragment() {
    // View Binding para acceder a las vistas sin errores de tipografía
    private var _binding: FragmentTareasBinding? = null
    private val binding get() = _binding!!

    private val tareas = mutableListOf<Tarea>()
    private lateinit var adapter: TareasAdapter
    private val materias = mutableListOf<Materia>()
    
    // Conexión con Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTareasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuramos la lista de tareas.
        binding.tareasRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TareasAdapter(tareas,
            onCheckedChange = { tarea, completada ->
                // Si el usuario marca la tarea como hecha, se actualiza en la nube de inmediato.
                val uid = auth.currentUser?.uid ?: return@TareasAdapter
                db.collection("users").document(uid).collection("tareas")
                    .document(tarea.id)
                    .update("completada", completada)
            },
            onClick = { tarea -> 
                // Si pulsa la tarea, le damos opciones de editar o borrar.
                mostrarOpcionesTarea(tarea) 
            }
        )
        binding.tareasRecycler.adapter = adapter

        // Botón de (+) para crear una nueva tarea.
        binding.tareasFab.setOnClickListener { mostrarDialogoTarea() }

        // Filtros rápidos: Todas, Pendientes, Listas.
        binding.tareasFilter.setOnCheckedStateChangeListener { _, _ ->
            cargarTareas()
        }

        // Gesto de jalar para actualizar.
        binding.tareasSwipeRefresh.setOnRefreshListener {
            binding.tareasSwipeRefresh.isRefreshing = false
        }

        cargarTareas()
        cargarMaterias()
    }

    /**
     * Traemos las materias para que el usuario no tenga que escribir el nombre a mano.
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
            }
    }

    /**
     * Jalamos las tareas de Firestore y aplicamos los filtros elegidos.
     */
    private fun cargarTareas() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("tareas")
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                tareas.clear()
                snapshots?.forEach { doc ->
                    tareas.add(Tarea.fromMap(doc.id, doc.data))
                }
                aplicarFiltro()
            }
    }

    /**
     * Lógica para mostrar solo las tareas que el usuario quiere ver en ese momento.
     */
    private fun aplicarFiltro() {
        val binding = _binding ?: return
        val pendientes = binding.tareasFilterPendientes.isChecked
        val completadas = binding.tareasFilterCompletadas.isChecked

        val filtradas = when {
            pendientes -> tareas.filter { !it.completada }
            completadas -> tareas.filter { it.completada }
            else -> tareas
        }

        // Refrescamos el adaptador con la lista filtrada.
        adapter = TareasAdapter(filtradas.toMutableList(),
            onCheckedChange = { tarea, completada ->
                val uid = auth.currentUser?.uid ?: return@TareasAdapter
                db.collection("users").document(uid).collection("tareas")
                    .document(tarea.id)
                    .update("completada", completada)
            },
            onClick = { tarea -> mostrarOpcionesTarea(tarea) }
        )
        binding.tareasRecycler.adapter = adapter

        // Si no hay tareas, mostramos un mensaje amigable.
        val isEmpty = filtradas.isEmpty()
        binding.tareasRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tareasEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    /**
     * Pequeño menú que sale al tocar una tarea.
     */
    private fun mostrarOpcionesTarea(tarea: Tarea) {
        val options = arrayOf("Editar", "Eliminar")
        AlertDialog.Builder(requireContext())
            .setTitle(tarea.titulo)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> mostrarDialogoTarea(tarea) // Reutilizamos el diálogo de crear para editar.
                    1 -> confirmarEliminarTarea(tarea)
                }
            }
            .show()
    }

    /**
     * Diálogo de confirmación antes de borrar una tarea de la base de datos.
     */
    private fun confirmarEliminarTarea(tarea: Tarea) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar tarea")
            .setMessage("¿Estás seguro de eliminar \"${tarea.titulo}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                val uid = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(uid).collection("tareas")
                    .document(tarea.id).delete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Ventana para agregar o editar tareas. Usamos BottomSheetDialog porque
     * se ve mucho más moderno que una pantalla completa.
     */
    private fun mostrarDialogoTarea(tarea: Tarea? = null) {
        val isEditing = tarea != null
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAgregarTareaBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.dialogTareaTitle.text = if (isEditing) "Editar Tarea" else "Agregar Tarea"

        // Si estamos editando, llenamos los campos con la info que ya tenemos.
        if (isEditing) {
            dialogBinding.dialogTareaTitulo.setText(tarea?.titulo)
            dialogBinding.dialogTareaDescripcion.setText(tarea?.descripcion)
            dialogBinding.dialogTareaMateria.setText(tarea?.materiaNombre, false)
            dialogBinding.dialogTareaFecha.setText(tarea?.fechaEntrega)
        }

        // Ponemos el autocompletado de materias.
        val materiaNombres = materias.map { it.nombre }
        val materiaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, materiaNombres)
        dialogBinding.dialogTareaMateria.setAdapter(materiaAdapter)

        // Al pulsar el campo de fecha, abrimos el calendario del sistema.
        dialogBinding.dialogTareaFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    dialogBinding.dialogTareaFecha.setText(
                        String.format("%02d/%02d/%d", day, month + 1, year)
                    )
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Guardar cambios o crear nueva tarea.
        dialogBinding.dialogTareaGuardar.setOnClickListener {
            val tituloText = dialogBinding.dialogTareaTitulo.text?.toString()?.trim() ?: ""
            val descripcionText = dialogBinding.dialogTareaDescripcion.text?.toString()?.trim() ?: ""
            val materiaText = dialogBinding.dialogTareaMateria.text?.toString()?.trim() ?: ""
            val fechaText = dialogBinding.dialogTareaFecha.text?.toString()?.trim() ?: ""

            if (tituloText.isEmpty()) {
                dialogBinding.dialogTareaTitulo.error = "Campo requerido"
                return@setOnClickListener
            }

            val materia = materias.find { it.nombre == materiaText }
            val materiaId = materia?.id ?: ""
            val materiaNombre = materia?.nombre ?: materiaText

            dialogBinding.dialogTareaGuardar.isEnabled = false

            // Armamos el mapa de datos para Firebase.
            val tareaData = mapOf(
                "materiaId" to materiaId,
                "materiaNombre" to materiaNombre,
                "titulo" to tituloText,
                "descripcion" to descripcionText,
                "fechaEntrega" to fechaText,
                "completada" to (tarea?.completada ?: false)
            )

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val ref = db.collection("users").document(uid).collection("tareas")

            if (isEditing) {
                ref.document(tarea!!.id).set(tareaData)
                    .addOnSuccessListener { dialog.dismiss() }
                    .addOnFailureListener { dialogBinding.dialogTareaGuardar.isEnabled = true }
            } else {
                ref.add(tareaData)
                    .addOnSuccessListener { dialog.dismiss() }
                    .addOnFailureListener { dialogBinding.dialogTareaGuardar.isEnabled = true }
            }
        }

        dialogBinding.dialogTareaCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
