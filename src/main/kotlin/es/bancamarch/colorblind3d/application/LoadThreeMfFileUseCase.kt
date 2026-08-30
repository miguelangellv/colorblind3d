package es.bancamarch.colorblind3d.application

import es.bancamarch.colorblind3d.domain.ThreeMfDocument
import es.bancamarch.colorblind3d.domain.ports.ThreeMfReader

/**
 * Caso de uso: cargar un fichero `.3mf` y obtener los colores únicos que usa el modelo.
 */
class LoadThreeMfFileUseCase(
    private val threeMfReader: ThreeMfReader,
) {
    suspend operator fun invoke(filePath: String): ThreeMfDocument = threeMfReader.read(filePath)
}
