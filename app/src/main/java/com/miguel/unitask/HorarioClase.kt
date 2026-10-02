package com.miguel.unitask

/**
 * Representa una clase en el horario semanal.
 * Guardamos el color de la materia para que el horario se vea igual que la lista de materias.
 */
data class HorarioClase(
    val id: String = "",
    val materiaId: String = "",
    val materiaNombre: String = "",
    val dia: Int = 1, // 1 para Lunes, 2 para Martes...
    val horaInicio: String = "",
    val horaFin: String = "",
    val aula: String = "",
    val color: Int = 0xFF6750A4.toInt()
) {
    /**
     * Prepara la clase para guardarla en la base de datos.
     */
    fun toMap() = mapOf(
        "materiaId" to materiaId,
        "materiaNombre" to materiaNombre,
        "dia" to dia,
        "horaInicio" to horaInicio,
        "horaFin" to horaFin,
        "aula" to aula,
        "color" to color
    )

    companion object {
        /**
         * Crea el objeto de clase con los datos que descargamos de Firebase.
         */
        fun fromMap(id: String, map: Map<String, Any?>?) = HorarioClase(
            id = id,
            materiaId = map?.get("materiaId") as? String ?: "",
            materiaNombre = map?.get("materiaNombre") as? String ?: "",
            dia = (map?.get("dia") as? Long)?.toInt() ?: 1,
            horaInicio = map?.get("horaInicio") as? String ?: "",
            horaFin = map?.get("horaFin") as? String ?: "",
            aula = map?.get("aula") as? String ?: "",
            color = (map?.get("color") as? Long)?.toInt() ?: 0xFF6750A4.toInt()
        )
    }
}
