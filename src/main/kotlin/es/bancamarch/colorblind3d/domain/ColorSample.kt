package es.bancamarch.colorblind3d.domain

/**
 * Un color detectado dentro de un modelo 3MF, con una etiqueta legible para el usuario
 * (p.ej. el nombre del material o "Color 1").
 */
data class ColorSample(
    val label: String,
    val rgb: RgbColor,
)
