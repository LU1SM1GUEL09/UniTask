package com.miguel.unitask

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox

/**
 * El Adaptador de Tareas. Es el encargado de convertir nuestra lista de datos
 * en tarjetas visuales que el usuario puede ver y tocar.
 */
class TareasAdapter(
    private val tareas: MutableList<Tarea>,
    private val onCheckedChange: (Tarea, Boolean) -> Unit, // Callback para el checkbox
    private val onClick: (Tarea) -> Unit // Callback para clics normales
) : RecyclerView.Adapter<TareasAdapter.ViewHolder>() {

    /**
     * El ViewHolder es como un "molde" que sostiene las vistas de una sola tarjeta.
     */
    inner class ViewHolder(private val view: MaterialCardView) : RecyclerView.ViewHolder(view) {
        private val cbCompletada: MaterialCheckBox = view.findViewById(R.id.tarea_item_check)
        private val tvTitulo: TextView = view.findViewById(R.id.tarea_item_titulo)
        private val tvMateria: TextView = view.findViewById(R.id.tarea_item_materia)
        private val tvFecha: TextView = view.findViewById(R.id.tarea_item_fecha)

        /**
         * Aquí llenamos el molde con la información de una tarea específica.
         */
        fun bind(tarea: Tarea) {
            cbCompletada.isChecked = tarea.completada
            tvTitulo.text = tarea.titulo
            tvMateria.text = tarea.materiaNombre
            tvFecha.text = tarea.fechaEntrega
            
            // Ponemos a escuchar el checkbox para marcar tareas como terminadas.
            cbCompletada.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(tarea, isChecked)
            }
            
            // Si el usuario toca la tarjeta, lanzamos el evento de clic.
            view.setOnClickListener { onClick(tarea) }
        }
    }

    /**
     * Inflamos el diseño XML de la tarjeta (item_tarea.xml).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarea, parent, false) as MaterialCardView
        return ViewHolder(view)
    }

    /**
     * Le decimos al ViewHolder qué tarea le toca dibujar según su posición en la lista.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tareas[position])
    }

    /**
     * Le dice al RecyclerView cuántos elementos tiene que dibujar en total.
     */
    override fun getItemCount(): Int = tareas.size
}
