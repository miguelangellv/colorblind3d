package es.bancamarch.colorblind3d.infrastructure.filamentcolors.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Marca que un fabricante concreto ha sido deshabilitado explícitamente por el usuario en el
 * selector de marcas. Solo se guardan los deshabilitados (no todos los habilitados): así, si
 * aparecen fabricantes nuevos en una descarga futura del catálogo, quedan habilitados por
 * defecto sin necesidad de migrar datos.
 */
@Entity(tableName = "disabled_manufacturers")
data class DisabledManufacturerEntity(
    @PrimaryKey val manufacturerName: String,
)
