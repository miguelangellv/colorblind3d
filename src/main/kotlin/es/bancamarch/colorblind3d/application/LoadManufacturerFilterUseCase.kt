package es.bancamarch.colorblind3d.application

import es.bancamarch.colorblind3d.domain.ports.ManufacturerPreferencesRepository

/** Recupera qué fabricantes ha deshabilitado el usuario en el selector de marcas (persistido). */
class LoadManufacturerFilterUseCase(
    private val manufacturerPreferencesRepository: ManufacturerPreferencesRepository,
) {
    suspend operator fun invoke(): Set<String> = manufacturerPreferencesRepository.loadDisabledManufacturers()
}
