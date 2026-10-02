package com.miguel.unitask

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miguel.unitask.databinding.DialogAgregarHorarioBinding
import com.miguel.unitask.databinding.FragmentHorarioBinding
import java.util.Calendar

/**
 * Fragmento de Horario. Aquí el estudiante ve sus clases de la semana.
 * Lo organizamos por tarjetas de Lunes a Viernes para que sea fácil de leer.
 */
class HorarioFragment : Fragment() {
    private var _binding: FragmentHorarioBinding? = null
    private val binding get() = _binding!!

    private val clases = mutableListOf<HorarioClase>()
    private val materias = mutableListOf<Materia>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lista de días para el selector
    private val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHorarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón flotante para agregar una nueva clase al horario.
        binding.horarioFab.setOnClickListener { mostrarDialogoHorario() }

        // El gesto de refrescar (SwipeRefresh) limpia el estado visual.
        binding.horarioSwipeRefresh.setOnRefreshListener {
            binding.horarioSwipeRefresh.isRefreshing = false
        }

        cargarHorario()
        cargarMaterias()
    }

    /**
     * Jalamos las materias para poder vincular una clase con una materia existente.
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
     * Traemos todas las clases del horario desde Firestore.
     */
    private fun cargarHorario() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("horario")
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                clases.clear()
                snapshots?.forEach { doc ->
                    clases.add(HorarioClase.fromMap(doc.id, doc.data))
                }
                // Una vez que tenemos los datos, pintamos las tarjetas.
                poblarHorario()
            }
    }

    private val diasNombres = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")

    /**
     * Esta es la función que "dibuja" el horario. 
     * Recorre cada tarjeta del layout y le pone las clases que corresponden a ese día.
     */
    private fun poblarHorario() {
        val cardIds = listOf(
            binding.horarioCardLunes,
            binding.horarioCardMartes,
            binding.horarioCardMiercoles,
            binding.horarioCardJueves,
            binding.horarioCardViernes
        )

        val labelIds = listOf(
            R.id.horario_text_lunes,
            R.id.horario_text_martes,
            R.id.horario_text_miercoles,
            R.id.horario_text_jueves,
            R.id.horario_text_viernes
        )

        for (i in 0..4) {
            val diaIndex = i + 1
            val diaClases = clases.filter { it.dia == diaIndex }
            val card = cardIds[i]
            val textView = card.findViewById<TextView>(labelIds[i])

            if (diaClases.isEmpty()) {
                textView.text = "Sin clases"
            } else {
                // Si hay clases, las unimos con un salto de línea (\n).
                textView.text = diaClases.joinToString("\n") { c ->
                    "${c.horaInicio}-${c.horaFin}  ${c.materiaNombre}${if (c.aula.isNotEmpty()) " (${c.aula})" else ""}"
                }
            }

            // Al tocar la tarjeta del día, mostramos el detalle de sus clases.
            card.setOnClickListener { mostrarClasesDelDia(diaIndex, diaClases) }
        }
    }

    /**
     * Si el día tiene clases, muestra una lista para elegir cuál editar o borrar.
     */
    private fun mostrarClasesDelDia(diaIndex: Int, diaClases: List<HorarioClase>) {
        if (diaClases.isEmpty()) {
            mostrarDialogoHorario()
            return
        }
        val nombres = diaClases.map { "${it.horaInicio}-${it.horaFin} ${it.materiaNombre}" }
        AlertDialog.Builder(requireContext())
            .setTitle(diasNombres[diaIndex - 1])
            .setItems(nombres.toTypedArray()) { _, which ->
                mostrarOpcionesClase(diaClases[which])
            }
            .show()
    }

    /**
     * Menú rápido para una clase específica.
     */
    private fun mostrarOpcionesClase(clase: HorarioClase) {
        val options = arrayOf("Editar", "Eliminar")
        AlertDialog.Builder(requireContext())
            .setTitle(clase.materiaNombre)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> mostrarDialogoHorario(clase)
                    1 -> confirmarEliminarClase(clase)
                }
            }
            .show()
    }

    /**
     * Diálogo de seguridad para borrar clases del horario.
     */
    private fun confirmarEliminarClase(clase: HorarioClase) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar clase")
            .setMessage("¿Eliminar ${clase.materiaNombre} (${clase.horaInicio}-${clase.horaFin})?")
            .setPositiveButton("Eliminar") { _, _ ->
                val uid = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(uid).collection("horario")
                    .document(clase.id).delete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Ventana emergente (BottomSheet) para gestionar el horario. 
     * Incluye selectores de tiempo (TimePicker).
     */
    private fun mostrarDialogoHorario(clase: HorarioClase? = null) {
        val isEditing = clase != null
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAgregarHorarioBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.dialogHorarioTitle.text = if (isEditing) "Editar Clase" else "Agregar Clase"

        if (isEditing) {
            dialogBinding.dialogHorarioMateria.setText(clase?.materiaNombre, false)
            dialogBinding.dialogHorarioDia.setText(dias.getOrElse(clase!!.dia - 1) { "" }, false)
            dialogBinding.dialogHorarioInicio.setText(clase?.horaInicio)
            dialogBinding.dialogHorarioFin.setText(clase?.horaFin)
            dialogBinding.dialogHorarioAula.setText(clase?.aula)
        }

        // Llenamos los adaptadores para los Spinners (Materia y Día).
        val materiaNombres = materias.map { it.nombre }
        val materiaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, materiaNombres)
        dialogBinding.dialogHorarioMateria.setAdapter(materiaAdapter)

        val diaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dias)
        dialogBinding.dialogHorarioDia.setAdapter(diaAdapter)

        // Selectores de hora para que el usuario no escriba mal el formato.
        dialogBinding.dialogHorarioInicio.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, m ->
                dialogBinding.dialogHorarioInicio.setText(String.format("%02d:%02d", h, m))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        dialogBinding.dialogHorarioFin.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, m ->
                dialogBinding.dialogHorarioFin.setText(String.format("%02d:%02d", h, m))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // Guardamos los datos en Firestore.
        dialogBinding.dialogHorarioGuardar.setOnClickListener {
            val materiaText = dialogBinding.dialogHorarioMateria.text?.toString()?.trim() ?: ""
            val diaText = dialogBinding.dialogHorarioDia.text?.toString()?.trim() ?: ""
            val inicioText = dialogBinding.dialogHorarioInicio.text?.toString()?.trim() ?: ""
            val finText = dialogBinding.dialogHorarioFin.text?.toString()?.trim() ?: ""
            val aulaText = dialogBinding.dialogHorarioAula.text?.toString()?.trim() ?: ""

            if (materiaText.isEmpty()) {
                dialogBinding.dialogHorarioMateria.error = "Campo requerido"
                return@setOnClickListener
            }

            val diaIndex = dias.indexOf(diaText) + 1
            if (diaIndex == 0) {
                dialogBinding.dialogHorarioDia.error = "Selecciona un día"
                return@setOnClickListener
            }

            val materia = materias.find { it.nombre == materiaText }
            dialogBinding.dialogHorarioGuardar.isEnabled = false

            val claseData = mapOf(
                "materiaId" to (materia?.id ?: ""),
                "materiaNombre" to (materia?.nombre ?: materiaText),
                "dia" to diaIndex,
                "horaInicio" to inicioText,
                "horaFin" to finText,
                "aula" to aulaText,
                "color" to (materia?.color ?: 0xFF6750A4.toInt())
            )

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val ref = db.collection("users").document(uid).collection("horario")

            if (isEditing) {
                ref.document(clase!!.id).set(claseData)
                    .addOnSuccessListener { dialog.dismiss() }
                    .addOnFailureListener { dialogBinding.dialogHorarioGuardar.isEnabled = true }
            } else {
                ref.add(claseData)
                    .addOnSuccessListener { dialog.dismiss() }
                    .addOnFailureListener { dialogBinding.dialogHorarioGuardar.isEnabled = true }
            }
        }

        dialogBinding.dialogHorarioCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
