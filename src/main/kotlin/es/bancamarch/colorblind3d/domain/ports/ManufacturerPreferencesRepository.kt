package es.bancamarch.colorblind3d.domain.ports

/**
 * Puerto de dominio para persistir qué fabricantes de filamento ha deshabilitado el usuario en
 * el selector de marcas, de forma que la selección se recuerde entre reinicios de la app. Solo
 * se guardan los deshabilitados: por defecto (nada guardado, o fabricantes nuevos) están todos
 * habilitados.
 */
interface ManufacturerPreferencesRepository {

    /** Nombres de los fabricantes que el usuario ha deshabilitado explícitamente. */
    suspend fun loadDisabledManufacturers(): Set<String>

    /** Sustituye el conjunto de fabricantes deshabilitados guardado por [disabled]. */
    suspend fun saveDisabledManufacturers(disabled: Set<String>)
}
