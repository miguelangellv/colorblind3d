package es.bancamarch.colorblind3d.infrastructure.threemf

import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Prueba manual de humo contra ficheros 3MF reales exportados por Bambu Studio disponibles en
 * el entorno de desarrollo. Se omite automáticamente si el fichero no existe (p.ej. en CI).
 */
class ThreeMfReaderSmokeTest {

    @Test
    fun `lee colores de varios 3MF reales de Bambu Studio si estan disponibles`() = runBlocking {
        val downloads = System.getProperty("user.home") + "/Descargas"
        val sampleNames = listOf(
            "Carabiner_1_ASA_20_4wall.3mf",
            "Wall_loop_hook.3mf",
            "Triangle_Sliced.3mf",
            "Ocarina_of_Time_N64_Songs_FINAL.3mf",
        )
        val reader = ThreeMfReaderImpl()
        var testedAtLeastOne = false

        sampleNames.forEach { name ->
            val samplePath = "$downloads/$name"
            if (!Path(samplePath).exists()) return@forEach
            testedAtLeastOne = true

            val document = reader.read(samplePath)
            println("Colores detectados en ${document.fileName}:")
            document.colors.forEach { println(" - ${it.label}: #${it.rgb.hex}") }

            assertTrue(document.colors.isNotEmpty(), "Se esperaba al menos un color en $name")
        }

        if (!testedAtLeastOne) return@runBlocking
    }
}
