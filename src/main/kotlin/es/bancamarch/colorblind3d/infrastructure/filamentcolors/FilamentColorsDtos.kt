package es.bancamarch.colorblind3d.infrastructure.filamentcolors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Respuesta paginada de `GET /api/swatch/` en filamentcolors.xyz. */
@Serializable
data class SwatchPageDto(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<SwatchDto> = emptyList(),
)

@Serializable
data class SwatchDto(
    val id: Int,
    val slug: String = "",
    @SerialName("color_name") val colorName: String = "",
    val manufacturer: ManufacturerDto? = null,
    @SerialName("filament_type") val filamentType: FilamentTypeDto? = null,
    @SerialName("hex_color") val hexColor: String = "000000",
    @SerialName("lab_l") val labL: Double? = null,
    @SerialName("lab_a") val labA: Double? = null,
    @SerialName("lab_b") val labB: Double? = null,
    @SerialName("image_front") val imageFront: String? = null,
    @SerialName("card_img") val cardImg: String? = null,
    @SerialName("amazon_purchase_link") val amazonPurchaseLink: String? = null,
    @SerialName("mfr_purchase_link") val mfrPurchaseLink: String? = null,
)

@Serializable
data class ManufacturerDto(
    val id: Int = 0,
    val name: String = "",
    val website: String? = null,
)

@Serializable
data class FilamentTypeDto(
    val id: Int = 0,
    val name: String = "",
)
