package com.miguel.unitask

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miguel.unitask.databinding.ItemMateriaBinding

/**
 * Adaptador para mostrar la cuadrícula de materias. 
 * Cada materia tiene su propio color que el usuario eligió.
 */
class MateriasAdapter(
    private val materias: List<Materia>,
    private val onClick: (Materia) -> Unit,
    private val onLongClick: (Materia) -> Unit
) : RecyclerView.Adapter<MateriasAdapter.MateriaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MateriaViewHolder {
        val binding = ItemMateriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MateriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MateriaViewHolder, position: Int) {
        holder.bind(materias[position])
    }

    override fun getItemCount() = materias.size

    inner class MateriaViewHolder(private val binding: ItemMateriaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(materia: Materia) {
            binding.materiaItemNombre.text = materia.nombre
            binding.materiaItemProfesor.text = materia.profesor
            
            // Pintamos la barrita lateral de la tarjeta con el color de la materia
            binding.materiaItemColor.backgroundTintList = ColorStateList.valueOf(materia.color)
            
            // Configuramos los clics
            binding.root.setOnClickListener { onClick(materia) }
            binding.root.setOnLongClickListener {
                onLongClick(materia)
                true
            }
        }
    }
}
