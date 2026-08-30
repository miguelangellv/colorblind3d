package es.bancamarch.colorblind3d.domain.ports

import es.bancamarch.colorblind3d.domain.FilamentMatch
import es.bancamarch.colorblind3d.domain.LabColor

/** Puerto de dominio para consultar el catálogo de filamentos de filamentcolors.xyz. */
interface FilamentColorRepository {

    /**
     * Se asegura de que el catálogo de swatches esté cargado (descargándolo y cacheándolo si
     * hace falta). Si [forceRefresh] es `true`, ignora la cache y vuelve a descargar.
     */
    suspend fun ensureCatalogLoaded(forceRefresh: Boolean = false)

    /** Número de swatches actualmente disponibles en el catálogo cacheado. */
    fun catalogSize(): Int

    /** Nombres de fabricante distintos presentes en el catálogo cacheado, ordenados alfabéticamente. */
    fun availableManufacturers(): List<String>

    /**
     * Devuelve los [limit] filamentos más cercanos a [target] (por distancia de color en Lab),
     * saltando los [offset] primeros. Permite implementar "más alternativas" sin red adicional.
     * Si [allowedManufacturers] no es `null`, sólo se consideran swatches de esos fabricantes.
     */
    fun bestMatches(
        target: LabColor,
        offset: Int,
        limit: Int,
        allowedManufacturers: Set<String>? = null,
    ): List<FilamentMatch>
}
