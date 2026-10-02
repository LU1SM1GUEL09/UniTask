package com.miguel.unitask

/**
 * Representa una Tarea o actividad pendiente.
 * Guardamos el ID de la materia para saber a cuál pertenece.
 */
data class Tarea(
    val id: String = "",
    val materiaId: String = "",
    val materiaNombre: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val fechaEntrega: String = "",
    val completada: Boolean = false
) {
    /**
     * Prepara los datos para subirlos a Firestore.
     */
    fun toMap() = mapOf(
        "materiaId" to materiaId,
        "materiaNombre" to materiaNombre,
        "titulo" to titulo,
        "descripcion" to descripcion,
        "fechaEntrega" to fechaEntrega,
        "completada" to completada
    )

    companion object {
        /**
         * Crea el objeto Tarea con la info que descargamos de la nube.
         */
        fun fromMap(id: String, map: Map<String, Any?>?) = Tarea(
            id = id,
            materiaId = map?.get("materiaId") as? String ?: "",
            materiaNombre = map?.get("materiaNombre") as? String ?: "",
            titulo = map?.get("titulo") as? String ?: "",
            descripcion = map?.get("descripcion") as? String ?: "",
            fechaEntrega = map?.get("fechaEntrega") as? String ?: "",
            completada = map?.get("completada") as? Boolean ?: false
        )
    }
}
