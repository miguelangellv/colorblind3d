package es.bancamarch.colorblind3d.application

import es.bancamarch.colorblind3d.domain.ColorSample
import es.bancamarch.colorblind3d.domain.FilamentMatch
import es.bancamarch.colorblind3d.domain.ports.FilamentColorRepository

/**
 * Caso de uso: pedir más alternativas de filamento para un color ya consultado, continuando la
 * lista de candidatos ordenada por distancia de color a partir de los que ya se han mostrado.
 */
class RequestMoreAlternativesUseCase(
    private val filamentColorRepository: FilamentColorRepository,
) {
    suspend operator fun invoke(
        color: ColorSample,
        alreadyShownCount: Int,
        count: Int = MatchFilamentsForColorUseCase.DEFAULT_MATCH_COUNT,
        allowedManufacturers: Set<String>? = null,
    ): List<FilamentMatch> {
        filamentColorRepository.ensureCatalogLoaded()
        return filamentColorRepository.bestMatches(
            color.rgb.toLab(),
            offset = alreadyShownCount,
            limit = count,
            allowedManufacturers = allowedManufacturers,
        )
    }
}
