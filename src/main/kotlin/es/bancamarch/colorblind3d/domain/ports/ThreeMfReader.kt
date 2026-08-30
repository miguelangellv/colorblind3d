package es.bancamarch.colorblind3d.domain.ports

import es.bancamarch.colorblind3d.domain.ThreeMfDocument

/** Puerto de dominio para leer un fichero 3MF y extraer sus colores. */
interface ThreeMfReader {
    /**
     * Lee el fichero 3MF ubicado en [filePath] (ruta absoluta del sistema de ficheros) y
     * devuelve el documento con los colores únicos encontrados.
     */
    suspend fun read(filePath: String): ThreeMfDocument
}
