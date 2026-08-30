package es.bancamarch.colorblind3d.application

import es.bancamarch.colorblind3d.domain.ports.FilamentColorRepository

/**
 * Caso de uso: obtener la lista de fabricantes de filamento presentes en el catálogo, para poder
 * mostrar un selector que permita habilitar/deshabilitar marcas concretas a la hora de buscar
 * coincidencias de color.
 */
class GetAvailableManufacturersUseCase(
    private val filamentColorRepository: FilamentColorRepository,
) {
    suspend operator fun invoke(): List<String> {
        filamentColorRepository.ensureCatalogLoaded()
        return filamentColorRepository.availableManufacturers()
    }
}
