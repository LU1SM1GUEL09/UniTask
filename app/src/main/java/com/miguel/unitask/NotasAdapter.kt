package com.miguel.unitask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miguel.unitask.databinding.ItemNotaBinding

/**
 * Adaptador para la lista de calificaciones. 
 * Muestra el tipo de nota, la materia y la calificación obtenida.
 */
class NotasAdapter(
    private val notas: List<Nota>,
    private val onClick: (Nota) -> Unit
) : RecyclerView.Adapter<NotasAdapter.NotaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val binding = ItemNotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        holder.bind(notas[position])
    }

    override fun getItemCount() = notas.size

    inner class NotaViewHolder(private val binding: ItemNotaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(nota: Nota) {
            binding.notaMateria.text = nota.materiaNombre
            binding.notaTipo.text = nota.tipo
            binding.notaCalificacion.text = nota.calificacion.toString()
            
            // Mostramos cuánto vale esta nota en el promedio (ej: 30%)
            binding.notaPorcentaje.text = "Peso: ${(nota.ponderacion * 100).toInt()}%"
            
            binding.root.setOnClickListener { onClick(nota) }
        }
    }
}
