package com.miguel.unitask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miguel.unitask.databinding.ItemExamenBinding

/**
 * Adaptador para la lista de exámenes. 
 * Muestra el título del examen, la materia y el aula.
 */
class ExamenesAdapter(
    private val examenes: List<Examen>,
    private val onClick: (Examen) -> Unit
) : RecyclerView.Adapter<ExamenesAdapter.ExamenViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamenViewHolder {
        val binding = ItemExamenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExamenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExamenViewHolder, position: Int) {
        holder.bind(examenes[position])
    }

    override fun getItemCount() = examenes.size

    inner class ExamenViewHolder(private val binding: ItemExamenBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(examen: Examen) {
            binding.examenTitulo.text = examen.titulo
            binding.examenMateria.text = examen.materiaNombre
            binding.examenFecha.text = examen.fecha
            
            // Si el examen tiene aula asignada, la mostramos; si no, ponemos un aviso.
            binding.examenAula.text = if (examen.aula.isNotEmpty()) "Aula: ${examen.aula}" else "Aula pendiente"
            
            binding.root.setOnClickListener { onClick(examen) }
        }
    }
}
