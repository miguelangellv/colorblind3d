package es.bancamarch.colorblind3d.infrastructure.filamentcolors

import es.bancamarch.colorblind3d.domain.RgbColor
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.openFilamentCacheDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Prueba manual de humo contra la API real de filamentcolors.xyz. Descarga el catálogo completo
 * y comprueba que se pueden calcular los mejores matches para un color de ejemplo.
 */
class FilamentColorRepositoryImplSmokeTest {

    @Test
    fun `descarga el catalogo real y calcula los mejores matches`() = runBlocking {
        val tempDbPath = kotlin.io.path.createTempFile(suffix = ".db").toString()
        val repository = FilamentColorRepositoryImpl(database = openFilamentCacheDatabase(tempDbPath))

        repository.ensureCatalogLoaded()
        assertTrue(repository.catalogSize() > 100, "Se esperaban cientos de swatches en el catálogo")

        val target = RgbColor.fromHex("FF0000").toLab()
        val matches = repository.bestMatches(target, offset = 0, limit = 5)
        matches.forEach {
            println(" - ${it.swatch.manufacturerName} ${it.swatch.colorName} (#${it.swatch.hex}) distancia=${it.distance}")
        }
        assertEquals(5, matches.size)

        val moreMatches = repository.bestMatches(target, offset = 5, limit = 5)
        assertEquals(5, moreMatches.size)
        assertTrue(matches.map { it.swatch.id }.none { it in moreMatches.map { it.swatch.id } })
    }
}
