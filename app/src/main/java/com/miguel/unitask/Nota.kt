package com.miguel.unitask

/**
 * Representa una calificación individual.
 * Guardamos la ponderación (el peso de la nota) para calcular el promedio final.
 */
data class Nota(
    val id: String = "",
    val materiaId: String = "",
    val materiaNombre: String = "",
    val tipo: String = "", // Ej: "Examen", "Tarea", etc.
    val calificacion: Double = 0.0,
    val ponderacion: Double = 1.0 // Por defecto vale el 100% si no se especifica.
) {
    /**
     * Mapea los datos para subirlos a la nube.
     */
    fun toMap() = mapOf(
        "materiaId" to materiaId,
        "materiaNombre" to materiaNombre,
        "tipo" to tipo,
        "calificacion" to calificacion,
        "ponderacion" to ponderacion
    )

    companion object {
        /**
         * Lee la nota desde el mapa de datos que nos manda Firebase.
         */
        fun fromMap(id: String, map: Map<String, Any?>?) = Nota(
            id = id,
            materiaId = map?.get("materiaId") as? String ?: "",
            materiaNombre = map?.get("materiaNombre") as? String ?: "",
            tipo = map?.get("tipo") as? String ?: "",
            // Aseguramos que la calificación se lea como Double.
            calificacion = (map?.get("calificacion") as? Number)?.toDouble() ?: 0.0,
            ponderacion = (map?.get("ponderacion") as? Number)?.toDouble() ?: 1.0
        )
    }
}
