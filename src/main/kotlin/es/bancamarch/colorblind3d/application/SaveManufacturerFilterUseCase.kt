package es.bancamarch.colorblind3d.application

import es.bancamarch.colorblind3d.domain.ports.ManufacturerPreferencesRepository

/** Guarda qué fabricantes ha deshabilitado el usuario en el selector de marcas, para recordarlo entre reinicios. */
class SaveManufacturerFilterUseCase(
    private val manufacturerPreferencesRepository: ManufacturerPreferencesRepository,
) {
    suspend operator fun invoke(disabled: Set<String>) {
        manufacturerPreferencesRepository.saveDisabledManufacturers(disabled)
    }
}
