package com.miguel.unitask

/**
 * Este es el modelo de una Materia. 
 * Lo usamos para representar cada asignatura que el estudiante inscribe.
 */
data class Materia(
    val id: String = "",
    val nombre: String = "",
    val profesor: String = "",
    val info: String = "",
    val color: Int = 0xFF6750A4.toInt() // Un color por defecto para que no se vea vacío.
) {
    /**
     * Convierte el objeto en un mapa para que Firebase lo pueda guardar fácil.
     */
    fun toMap() = mapOf(
        "nombre" to nombre,
        "profesor" to profesor,
        "info" to info,
        "color" to color
    )

    companion object {
        /**
         * Reconstruye la materia a partir de los datos que vienen de la base de datos.
         */
        fun fromMap(id: String, map: Map<String, Any?>?) = Materia(
            id = id,
            nombre = map?.get("nombre") as? String ?: "",
            profesor = map?.get("profesor") as? String ?: "",
            info = map?.get("info") as? String ?: "",
            // Firebase guarda los números como Long, por eso hacemos la conversión.
            color = (map?.get("color") as? Long)?.toInt() ?: 0xFF6750A4.toInt()
        )
    }
}
