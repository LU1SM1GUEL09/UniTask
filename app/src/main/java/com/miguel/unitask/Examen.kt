package com.miguel.unitask

/**
 * Modelo para los Exámenes. 
 * Además de la fecha, guardamos el aula para que el alumno no se pierda el día del parcial.
 */
data class Examen(
    val id: String = "",
    val materiaId: String = "",
    val materiaNombre: String = "",
    val titulo: String = "",
    val fecha: String = "",
    val aula: String = "",
    val completado: Boolean = false
) {
    /**
     * Convierte los datos a un formato que Firebase Firestore entienda.
     */
    fun toMap() = mapOf(
        "materiaId" to materiaId,
        "materiaNombre" to materiaNombre,
        "titulo" to titulo,
        "fecha" to fecha,
        "aula" to aula,
        "completado" to completado
    )

    companion object {
        /**
         * Reconstruye el examen con la información que viene de la base de datos.
         */
        fun fromMap(id: String, map: Map<String, Any?>?) = Examen(
            id = id,
            materiaId = map?.get("materiaId") as? String ?: "",
            materiaNombre = map?.get("materiaNombre") as? String ?: "",
            titulo = map?.get("titulo") as? String ?: "",
            fecha = map?.get("fecha") as? String ?: "",
            aula = map?.get("aula") as? String ?: "",
            completado = map?.get("completado") as? Boolean ?: false
        )
    }
}
