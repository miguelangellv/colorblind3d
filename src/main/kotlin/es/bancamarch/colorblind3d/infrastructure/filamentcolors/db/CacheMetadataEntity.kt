package es.bancamarch.colorblind3d.infrastructure.filamentcolors.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila única (id fijo = 0) que guarda cuándo se descargó por última vez el catálogo completo de
 * filamentcolors.xyz, para poder aplicar el TTL de la cache sin depender de la fecha de
 * modificación del fichero de base de datos.
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val downloadedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
