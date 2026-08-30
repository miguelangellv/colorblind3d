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
 *    asignado a cada objeto) y con el atributo `paint_color` de cada `<triangle>` en los
 *    ficheros `.model` bajo `3D/` (pintado multicolor / MMU painting), para quedarnos solo con los
 *    colores realmente usados, tanto los de base como los pintados.
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

        val paintedExtruders = extractPaintedExtruders(zip)

        val extrudersToUse = (usedExtruders + paintedExtruders).ifEmpty { (1..paletteColors.size).toSet() }

        return extrudersToUse
            .sorted()
            .mapNotNull { extruderNumber ->
                val index = extruderNumber - 1
                val hex = paletteColors.getOrNull(index) ?: return@mapNotNull null
                val type = paletteTypes.getOrNull(index)
                val isOnlyPainted = extruderNumber in paintedExtruders && extruderNumber !in usedExtruders
                val label = buildString {
                    append("Filamento $extruderNumber")
                    val details = listOfNotNull(
                        type?.takeIf { it.isNotBlank() },
                        "pintado".takeIf { isOnlyPainted },
                    )
                    if (details.isNotEmpty()) {
                        append(" (${details.joinToString(", ")})")
                    }
                }
                ColorSample(label = label, rgb = RgbColor.fromHex(hex))
            }
            .distinctBy { it.rgb.hex }
    }

    /**
     * Busca en todos los ficheros `.model` bajo `3D/` del contenedor los atributos `paint_color` de
     * cada `<triangle>` (pintado multicolor / MMU painting de Bambu Studio y PrusaSlicer) y
     * devuelve el conjunto de números de extrusor usados en el pintado, aunque el objeto tenga
     * asignado como base un extrusor distinto (o ninguno).
     */
    private fun extractPaintedExtruders(zip: SimpleZipReader): Set<Int> {
        val painted = mutableSetOf<Int>()
        zip.entryNames()
            .filter { it.startsWith("3D/") && it.endsWith(".model") }
            .forEach { entryName ->
                val xml = zip.readText(entryName)
                KtXmlReader(xml, relaxed = true, expandEntities = true).use { reader ->
                    while (reader.hasNext()) {
                        val event = reader.next()
                        if (event == EventType.START_ELEMENT && reader.localName == "triangle") {
                            val paintColor = reader.getAttributeValue(null, "paint_color")
                            if (!paintColor.isNullOrBlank()) {
                                decodePaintColorExtruders(paintColor, painted)
                            }
                        }
                    }
                }
            }
        return painted
    }

    /**
     * Decodifica la codificación de árbol binario (una cifra hexadecimal por nodo) que Bambu
     * Studio/PrusaSlicer usan para serializar el estado de pintado de cada triángulo original en
     * el atributo `paint_color`, y añade a [result] los números de extrusor (1-indexados)
     * encontrados en las hojas del árbol.
     *
     * Formato (ver `TriangleSelector::serialize`/`deserialize` de PrusaSlicer/Bambu Studio): la
     * cadena se genera insertando cada cifra al principio, por lo que para leerla en orden
     * cronológico basta con invertirla; cada cifra es entonces directamente el "nibble" original
     * de 4 bits. Un nodo hoja (2 bits bajos = 0) codifica su estado en los 2 bits altos, salvo que
     * valgan `11`, en cuyo caso el valor real (extrusor - 3) viene en nibbles adicionales
     * (terminados por el primer nibble distinto de `1111`). Un nodo dividido (2 bits bajos != 0)
     * indica cuántos hijos hay que decodificar recursivamente a continuación.
     */
    private fun decodePaintColorExtruders(paintColor: String, result: MutableSet<Int>) {
        val codes = paintColor.reversed().map {
            Character.digit(it, 16).takeIf { digit -> digit in 0..15 } ?: return
        }
        val index = intArrayOf(0)
        decodePaintNode(codes, index, result)
    }

    private fun decodePaintNode(codes: List<Int>, index: IntArray, result: MutableSet<Int>) {
        if (index[0] >= codes.size) return
        val code = codes[index[0]]
        index[0]++
        val splitSides = code and 0b11
        if (splitSides == 0) {
            val state = if (code and 0b1100 == 0b1100) {
                var extraNibbles = 0
                var nextCode = codes.getOrNull(index[0])?.also { index[0]++ } ?: 0
                while (nextCode == 0b1111) {
                    extraNibbles++
                    nextCode = codes.getOrNull(index[0])?.also { index[0]++ } ?: break
                }
                nextCode + 15 * extraNibbles + 3
            } else {
                code shr 2
            }
            if (state > 0) result += state
        } else {
            repeat(splitSides + 1) { decodePaintNode(codes, index, result) }
        }
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
