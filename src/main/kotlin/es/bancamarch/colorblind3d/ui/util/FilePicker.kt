package es.bancamarch.colorblind3d.ui.util

import java.awt.FileDialog
import java.awt.Frame

/**
 * Abre el selector de ficheros nativo del sistema operativo filtrado a `.3mf`. Se apoya en
 * `java.awt.FileDialog` porque no existe una alternativa Kotlin-nativa para el diálogo nativo del
 * sistema operativo; esto es independiente de la carga/parseo del contenido del fichero, que se
 * hace con Okio + xmlutil en `infrastructure.threemf`.
 */
fun pickThreeMfFile(): String? {
    val dialog = FileDialog(null as Frame?, "Abrir fichero 3MF", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".3mf", ignoreCase = true) }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return dialog.directory + file
}
