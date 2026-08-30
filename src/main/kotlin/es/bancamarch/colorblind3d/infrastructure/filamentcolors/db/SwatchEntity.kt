package es.bancamarch.colorblind3d.infrastructure.filamentcolors.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import es.bancamarch.colorblind3d.domain.FilamentSwatch
import es.bancamarch.colorblind3d.domain.LabColor

/**
 * Fila de la tabla `swatches`: representación en SQLite (vía Room) de una ficha de filamento tal
 * y como la devuelve la API de filamentcolors.xyz. Es un simple espejo de [FilamentSwatch] para
 * poder persistirlo; la conversión a/desde el modelo de dominio vive en las funciones de
 * extensión de abajo.
 */
@Entity(tableName = "swatches")
data class SwatchEntity(
    @PrimaryKey val id: Int,
    val slug: String,
    val colorName: String,
    val manufacturerName: String,
    val filamentType: String,
    val hex: String,
    val labL: Double,
    val labA: Double,
    val labB: Double,
    val imageFrontUrl: String?,
    val cardImageUrl: String?,
    val purchaseUrl: String?,
)

fun SwatchEntity.toDomain(): FilamentSwatch = FilamentSwatch(
    id = id,
    slug = slug,
    colorName = colorName,
    manufacturerName = manufacturerName,
    filamentType = filamentType,
    hex = hex,
    lab = LabColor(labL, labA, labB),
    imageFrontUrl = imageFrontUrl,
    cardImageUrl = cardImageUrl,
    purchaseUrl = purchaseUrl,
)

fun FilamentSwatch.toEntity(): SwatchEntity = SwatchEntity(
    id = id,
    slug = slug,
    colorName = colorName,
    manufacturerName = manufacturerName,
    filamentType = filamentType,
    hex = hex,
    labL = lab.l,
    labA = lab.a,
    labB = lab.b,
    imageFrontUrl = imageFrontUrl,
    cardImageUrl = cardImageUrl,
    purchaseUrl = purchaseUrl,
)
