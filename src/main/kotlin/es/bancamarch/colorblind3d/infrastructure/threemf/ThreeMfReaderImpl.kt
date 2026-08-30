package es.bancamarch.colorblind3d.infrastructure.threemf

import es.bancamarch.colorblind3d.domain.ColorSample
import es.bancamarch.colorblind3d.domain.RgbColor
import es.bancamarch.colorblind3d.domain.ThreeMfDocument
import es.bancamarch.colorblind3d.domain.ThreeMfParsingException
import es.bancamarch.colorblind3d.domain.ports.ThreeMfReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.core.KtXmlReader
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Lee ficheros `.3mf` (en particular los exportados por Bambu Studio) usando Okio para acceder
 * al fichero (a través de [SimpleZipReader], un lector de ZIP en Kotlin construido sobre el
 * acceso posicional de Okio, en vez de `java.util.zip.ZipFile`/`ZipInputStream`) y
 * xmlutil/kotlinx.serialization para el parseo del XML/JSON embebido (en vez de
 * `javax.xml.parsers`).
 *
 * Estrategia de extracción de color, de más a menos específica:
 * 1. Bambu Studio: `Metadata/project_settings.config` (JSON con la paleta de filamentos del
 *    proyecto) cruzado con `Metadata/model_settings.config` (XML con el extrusor/filamento
 *    asignado a cada objeto), para quedarnos solo con los colores realmente usados.
 * 2. Genérico 3MF: `<basematerials>` con `displaycolor` dentro de `3D/3dmodel.model`.
 */
class ThreeMfReaderImpl : ThreeMfReader {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun read(filePath: String): ThreeMfDocument = withContext(Dispatchers.IO) {
        val zipPath = filePath.toPath()
        if (!FileSystem.SYSTEM.exists(zipPath)) {
            throw ThreeMfParsingException("El fichero no existe: $filePath")
        }

        val zipReader = try {
            SimpleZipReader(FileSystem.SYSTEM.openReadOnly(zipPath))
        } catch (e: Exception) {
            throw ThreeMfParsingException("El fichero no es un .3mf/ZIP válido: $filePath", e)
        }

        val colors = try {
            zipReader.use {
                extractBambuStudioColors(it) ?: extractGenericBaseMaterialColors(it)
            }
        } catch (e: Exception) {
            throw ThreeMfParsingException("No se pudieron leer los colores del fichero 3MF", e)
        }

        if (colors.isEmpty()) {
            throw ThreeMfParsingException("No se ha encontrado ningún color en el fichero 3MF")
        }

        ThreeMfDocument(fileName = zipPath.name, colors = colors)
    }

    /** Intenta extraer los colores usando el formato específico de Bambu Studio. Null si no aplica. */
    private fun extractBambuStudioColors(zip: SimpleZipReader): List<ColorSample>? {
        val projectSettingsName = "Metadata/project_settings.config"
        if (!zip.exists(projectSettingsName)) return null

        val projectJson = zip.readText(projectSettingsName)
        val root = json.parseToJsonElement(projectJson).jsonObject
        val paletteColors = root["filament_colour"]?.asStringList() ?: return null
        val paletteTypes = root["filament_type"]?.asStringList() ?: emptyList()

        val modelSettingsName = "Metadata/model_settings.config"
        val usedExtruders = if (zip.exists(modelSettingsName)) {
            parseUsedExtruders(zip.readText(modelSettingsName))
        } else {
            emptySet()
        }

        val extrudersToUse = usedExtruders.ifEmpty { (1..paletteColors.size).toSet() }

        return extrudersToUse
            .sorted()
            .mapNotNull { extruderNumber ->
                val index = extruderNumber - 1
                val hex = paletteColors.getOrNull(index) ?: return@mapNotNull null
                val type = paletteTypes.getOrNull(index)
                val label = if (type.isNullOrBlank()) {
                    "Filamento $extruderNumber"
                } else {
                    "Filamento $extruderNumber ($type)"
                }
                ColorSample(label = label, rgb = RgbColor.fromHex(hex))
            }
            .distinctBy { it.rgb.hex }
    }

    /** Extrae los colores de `<basematerials>` del modelo 3MF genérico (fallback). */
    private fun extractGenericBaseMaterialColors(zip: SimpleZipReader): List<ColorSample> {
        val modelName = "3D/3dmodel.model"
        if (!zip.exists(modelName)) return emptyList()

        val modelXml = zip.readText(modelName)
        val result = mutableListOf<ColorSample>()
        var materialIndex = 0

        KtXmlReader(modelXml, relaxed = true, expandEntities = true).use { reader ->
            while (reader.hasNext()) {
                val event = reader.next()
                if (event == EventType.START_ELEMENT && reader.localName == "base") {
                    val displayColor = reader.getAttributeValue(null, "displaycolor")
                    if (!displayColor.isNullOrBlank()) {
                        materialIndex++
                        val name = reader.getAttributeValue(null, "name") ?: "Material $materialIndex"
                        result += ColorSample(label = name, rgb = RgbColor.fromHex(displayColor))
                    }
                }
            }
        }
        return result.distinctBy { it.rgb.hex }
    }

    /** Recorre `Metadata/model_settings.config` buscando `<metadata key="extruder" value="N"/>`. */
    private fun parseUsedExtruders(xml: String): Set<Int> {
        val extruders = mutableSetOf<Int>()
        KtXmlReader(xml, relaxed = true, expandEntities = true).use { reader ->
            while (reader.hasNext()) {
                val event = reader.next()
                if (event == EventType.START_ELEMENT && reader.localName == "metadata") {
                    val key = reader.getAttributeValue(null, "key")
                    if (key == "extruder") {
                        reader.getAttributeValue(null, "value")?.toIntOrNull()?.let { extruders += it }
                    }
                }
            }
        }
        return extruders
    }

    private fun JsonElement.asStringList(): List<String> =
        jsonArray.map { it.jsonPrimitive.content }
}
