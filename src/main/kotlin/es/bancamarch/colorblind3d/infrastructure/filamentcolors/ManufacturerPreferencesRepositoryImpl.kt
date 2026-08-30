package es.bancamarch.colorblind3d.infrastructure.filamentcolors

import es.bancamarch.colorblind3d.domain.ports.ManufacturerPreferencesRepository
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.FilamentCacheDatabase
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.defaultFilamentCacheDatabasePath
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.openFilamentCacheDatabase

/**
 * Implementación de [ManufacturerPreferencesRepository] que guarda los fabricantes deshabilitados
 * en la misma base de datos Room/SQLite usada como cache del catálogo de filamentcolors.xyz.
 */
class ManufacturerPreferencesRepositoryImpl(
    private val database: FilamentCacheDatabase = openFilamentCacheDatabase(defaultFilamentCacheDatabasePath()),
) : ManufacturerPreferencesRepository {

    override suspend fun loadDisabledManufacturers(): Set<String> =
        database.disabledManufacturerDao().getAll().toSet()

    override suspend fun saveDisabledManufacturers(disabled: Set<String>) {
        database.disabledManufacturerDao().replaceAll(disabled)
    }
}
