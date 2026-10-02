package com.miguel.unitask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miguel.unitask.databinding.FragmentDashboardBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Este es el fragmento principal. Aquí el estudiante ve un resumen de toda su vida académica.
 * Lo armamos para que sea lo primero que veas al entrar.
 */
class DashboardFragment : Fragment() {
    // Usamos View Binding para no pelearnos con el findViewById y que el código sea más limpio
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Instancias de Firebase para jalar los datos de la nube
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Listas temporales para guardar lo que traemos de internet
    private val tareas = mutableListOf<Tarea>()
    private val examenes = mutableListOf<Examen>()
    private val notas = mutableListOf<Nota>()
    
    // Variables para controlar qué mes estamos viendo en el calendario
    private var calendarioMes = 0
    private var calendarioAno = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuramos la fecha de hoy con un formato amigable en español
        val calendar = Calendar.getInstance()
        calendarioMes = calendar.get(Calendar.MONTH)
        calendarioAno = calendar.get(Calendar.YEAR)
        val locale = Locale.forLanguageTag("es")
        val dateFormat = SimpleDateFormat("EEEE d 'de' MMMM", locale)
        
        // Saludamos al usuario por su nombre (si no hay nombre, le decimos "Usuario")
        val userName = auth.currentUser?.displayName ?: "Usuario"
        binding.dashboardWelcome.text = getString(R.string.dashboard_welcome, userName)

        binding.dashboardDate.text = dateFormat.format(calendar.time).replaceFirstChar { it.uppercase() }

        actualizarTituloMes()

        // Botones para navegar entre los meses del calendario
        binding.dashboardCalendarPrev.setOnClickListener {
            calendarioMes--
            if (calendarioMes < 0) { calendarioMes = 11; calendarioAno-- }
            actualizarTituloMes()
            actualizarCalendario()
        }
        binding.dashboardCalendarNext.setOnClickListener {
            calendarioMes++
            if (calendarioMes > 11) { calendarioMes = 0; calendarioAno++ }
            actualizarTituloMes()
            actualizarCalendario()
        }

        // Configuración de los clics para ir a otras secciones
        binding.dashboardCalendarVer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_examenes)
        }

        // El menú de abajo (BottomNav) para movernos por la app
        binding.dashboardBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboardFragment -> true
                R.id.materiasFragment -> {
                    findNavController().navigate(R.id.action_dashboard_to_materias); true
                }
                R.id.tareasFragment -> {
                    findNavController().navigate(R.id.action_dashboard_to_tareas); true
                }
                R.id.perfilFragment -> {
                    findNavController().navigate(R.id.action_dashboard_to_perfil); true
                }
                else -> false
            }
        }

        // Accesos rápidos de las tarjetas de estadísticas
        binding.dashboardStatEntrega.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tareas)
        }
        binding.dashboardStatExamen.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_examenes)
        }
        binding.dashboardStatPromedio.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_notas)
        }
        binding.dashboardStatTareas.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_tareas)
        }
        binding.dashboardStatHorario.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_horario)
        }
        
        // Aquí abrimos la nueva sección de noticias que acabamos de integrar
        binding.dashboardStatNoticias.setOnClickListener {
            val intent = android.content.Intent(requireContext(), NoticiasActivity::class.java)
            startActivity(intent)
        }

        cargarDatos()
    }

    /**
     * Muestra el mes y año actual en la tarjeta del calendario
     */
    private fun actualizarTituloMes() {
        val locale = Locale.forLanguageTag("es")
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, calendarioMes)
        cal.set(Calendar.YEAR, calendarioAno)
        val monthFormat = SimpleDateFormat("MMMM yyyy", locale)
        binding.dashboardCalendarMonth.text = monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }
    }

    /**
     * Traemos toda la info de Firebase. Usamos SnapshotListeners para que si algo
     * cambia en la base de datos, la pantalla se actualice solita sin recargar.
     */
    private fun cargarDatos() {
        val uid = auth.currentUser?.uid ?: return

        // Escuchamos las tareas pendientes
        db.collection("users").document(uid).collection("tareas")
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                tareas.clear()
                snapshots?.forEach { doc ->
                    tareas.add(Tarea.fromMap(doc.id, doc.data))
                }
                actualizarStatsTareas()
                actualizarCalendario()
            }

        // Escuchamos los exámenes
        db.collection("users").document(uid).collection("examenes")
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                examenes.clear()
                snapshots?.forEach { doc ->
                    examenes.add(Examen.fromMap(doc.id, doc.data))
                }
                actualizarStatsExamenes()
                actualizarCalendario()
            }

        // Escuchamos las notas para sacar el promedio
        db.collection("users").document(uid).collection("notas")
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                notas.clear()
                snapshots?.forEach { doc ->
                    notas.add(Nota.fromMap(doc.id, doc.data))
                }
                actualizarPromedio()
            }
    }

    /**
     * Calcula cuántas tareas faltan y muestra la fecha de la más cercana.
     */
    private fun actualizarStatsTareas() {
        val binding = _binding ?: return
        val pendientes = tareas.count { !it.completada }
        binding.dashboardStatTareasNum.text = pendientes.toString()
        binding.dashboardStatTareasSub.text = if (pendientes == 1) {
            getString(R.string.stats_tareas_pendientes_count, 1)
        } else {
            getString(R.string.stats_tareas_pendientes_count, pendientes)
        }

        val proxima = tareas
            .filter { !it.completada && it.fechaEntrega.isNotEmpty() }
            .minByOrNull { it.fechaEntrega }
        if (proxima != null) {
            binding.dashboardStatEntregaNum.text = proxima.fechaEntrega.take(5)
            binding.dashboardStatEntregaSub.text = proxima.materiaNombre.ifEmpty { getString(R.string.stats_proxima_entrega) }
        } else {
            binding.dashboardStatEntregaNum.text = "--"
            binding.dashboardStatEntregaSub.text = getString(R.string.stats_sin_tareas)
        }
    }

    /**
     * Busca el examen más próximo para avisarle al estudiante.
     */
    private fun actualizarStatsExamenes() {
        val binding = _binding ?: return
        val pendientes = examenes.count { !it.completado }
        val proximo = examenes
            .filter { !it.completado && it.fecha.isNotEmpty() }
            .minByOrNull { it.fecha }
        if (proximo != null) {
            binding.dashboardStatExamenNum.text = proximo.fecha.take(5)
            binding.dashboardStatExamenSub.text = proximo.materiaNombre.ifEmpty { getString(R.string.stats_proximo_examen) }
        } else {
            binding.dashboardStatExamenNum.text = "--"
            binding.dashboardStatExamenSub.text = if (pendientes == 0) getString(R.string.stats_sin_examenes) else getString(R.string.stats_proximo_examen)
        }
    }

    /**
     * Saca el promedio ponderado de las notas que el usuario ha subido.
     */
    private fun actualizarPromedio() {
        val binding = _binding ?: return
        if (notas.isEmpty()) {
            binding.dashboardStatPromedioNum.text = "0.0"
            binding.dashboardStatPromedioSub.text = getString(R.string.stats_sin_notas)
            return
        }
        val suma = notas.sumOf { it.calificacion * it.ponderacion }
        val pesos = notas.sumOf { it.ponderacion }
        val promedio = if (pesos > 0) suma / pesos else 0.0
        binding.dashboardStatPromedioNum.text = String.format(Locale.getDefault(), "%.1f", promedio)
        binding.dashboardStatPromedioSub.text = getString(R.string.stats_notas_count, notas.size)
    }

    /**
     * Esta función revisa qué días del mes tienen tareas o exámenes para ponerles los puntitos.
     */
    private fun actualizarCalendario() {
        val diasConEventos = mutableSetOf<Int>()

        tareas.filter { !it.completada && it.fechaEntrega.isNotEmpty() }.forEach { t ->
            try {
                val parts = t.fechaEntrega.split("/")
                if (parts.size == 3) {
                    val dia = parts[0].toInt()
                    val mes = parts[1].toInt() - 1
                    val ano = parts[2].toInt()
                    if (mes == calendarioMes && ano == calendarioAno) {
                        diasConEventos.add(dia)
                    }
                }
            } catch (_: Exception) {}
        }
        examenes.filter { !it.completado && it.fecha.isNotEmpty() }.forEach { e ->
            try {
                val parts = e.fecha.split("/")
                if (parts.size == 3) {
                    val dia = parts[0].toInt()
                    val mes = parts[1].toInt() - 1
                    val ano = parts[2].toInt()
                    if (mes == calendarioMes && ano == calendarioAno) {
                        diasConEventos.add(dia)
                    }
                }
            } catch (_: Exception) {}
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, calendarioMes)
        calendar.set(Calendar.YEAR, calendarioAno)
        buildCalendarGrid(calendar, diasConEventos)
    }

    /**
     * Dibuja el grid del calendario. Es un poco de carpintería con código para que 
     * los días caigan donde deben según el mes.
     */
    private fun buildCalendarGrid(calendar: Calendar, eventDays: Set<Int>) {
        val binding = _binding ?: return
        val context = context ?: return
        val grid = binding.dashboardCalendarGrid
        grid.removeAllViews()

        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        // Espacios vacíos si el mes no empieza en lunes
        repeat(offset) {
            val emptyView = View(context)
            emptyView.layoutParams = GridLayout.LayoutParams().apply {
                width = 0; height = dpToPx(40)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            grid.addView(emptyView)
        }

        // Llenamos los días del mes
        for (day in 1..daysInMonth) {
            val dayView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, grid, false)
            val tvDay = dayView.findViewById<android.widget.TextView>(R.id.calendar_day_num)
            tvDay.text = day.toString()

            // Si es hoy, lo pintamos de un color diferente para que resalte
            if (day == today && calendarioMes == Calendar.getInstance().get(Calendar.MONTH) && calendarioAno == Calendar.getInstance().get(Calendar.YEAR)) {
                dayView.setBackgroundResource(R.drawable.calendar_today_bg)
                tvDay.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            // Si el día tiene una tarea o examen, le ponemos el puntito indicador
            if (day in eventDays) {
                dayView.findViewById<View>(R.id.calendar_day_dot).visibility = View.VISIBLE
            }

            dayView.layoutParams = GridLayout.LayoutParams().apply {
                width = 0; height = dpToPx(40)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            grid.addView(dayView)
        }
    }

    // Una función de ayuda para convertir DP a Píxeles y que el diseño no se rompa
    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
