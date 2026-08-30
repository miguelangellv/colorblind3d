package es.bancamarch.colorblind3d.application

import es.bancamarch.colorblind3d.domain.ColorSample
import es.bancamarch.colorblind3d.domain.FilamentMatch
import es.bancamarch.colorblind3d.domain.ports.FilamentColorRepository

/**
 * Caso de uso: buscar, para un color detectado en el modelo, los N filamentos reales que mejor
 * coinciden en color (más cercanos en distancia perceptual Lab). Se encarga también de asegurar
 * que el catálogo de filamentcolors.xyz esté cargado antes de calcular los matches.
 */
class MatchFilamentsForColorUseCase(
    private val filamentColorRepository: FilamentColorRepository,
) {
    suspend operator fun invoke(
        color: ColorSample,
        count: Int = DEFAULT_MATCH_COUNT,
        allowedManufacturers: Set<String>? = null,
    ): List<FilamentMatch> {
        filamentColorRepository.ensureCatalogLoaded()
        return filamentColorRepository.bestMatches(
            color.rgb.toLab(),
            offset = 0,
            limit = count,
            allowedManufacturers = allowedManufacturers,
        )
    }

    companion object {
        const val DEFAULT_MATCH_COUNT = 5
    }
}
