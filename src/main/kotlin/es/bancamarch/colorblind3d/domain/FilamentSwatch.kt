package es.bancamarch.colorblind3d.domain

/**
 * Ficha de un filamento real, tal y como la expone la API pública de filamentcolors.xyz.
 */
data class FilamentSwatch(
    val id: Int,
    val slug: String,
    val colorName: String,
    val manufacturerName: String,
    val filamentType: String,
    val hex: String,
    val lab: LabColor,
    val imageFrontUrl: String?,
    val cardImageUrl: String?,
    val purchaseUrl: String?,
)

/** Un candidato de filamento para un [ColorSample], con la distancia de color respecto al objetivo. */
data class FilamentMatch(
    val swatch: FilamentSwatch,
    val distance: Double,
)
