package es.bancamarch.colorblind3d.domain

/** Documento 3MF cargado, con los colores únicos detectados en el modelo. */
data class ThreeMfDocument(
    val fileName: String,
    val colors: List<ColorSample>,
)
