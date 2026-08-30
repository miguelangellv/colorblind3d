package es.bancamarch.colorblind3d.infrastructure.filamentcolors.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos Room (SQLite embebido, sin dependencias de Android) usada como cache local del
 * catálogo de filamentcolors.xyz. Se instancia mediante [openFilamentCacheDatabase].
 */
@Database(
    entities = [SwatchEntity::class, CacheMetadataEntity::class, DisabledManufacturerEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class FilamentCacheDatabase : RoomDatabase() {
    abstract fun swatchDao(): SwatchDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun disabledManufacturerDao(): DisabledManufacturerDao
}
