package es.bancamarch.colorblind3d.application

import es.bancamarch.colorblind3d.domain.ports.FilamentColorRepository

/**
 * Caso de uso: precargar el catálogo de filamentos de filamentcolors.xyz (descarga + cache en
 * disco) para que, cuando el usuario abra un fichero 3MF, las coincidencias de color se puedan
 * calcular al instante sin esperar a la descarga.
 */
class PreloadFilamentCatalogUseCase(
    private val filamentColorRepository: FilamentColorRepository,
) {
    suspend operator fun invoke() {
        filamentColorRepository.ensureCatalogLoaded()
    }
}
