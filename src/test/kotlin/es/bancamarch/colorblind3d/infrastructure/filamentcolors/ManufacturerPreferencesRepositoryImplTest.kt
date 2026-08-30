package es.bancamarch.colorblind3d.infrastructure.filamentcolors

import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.openFilamentCacheDatabase
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifica que la preferencia de fabricantes deshabilitados sobrevive a "reiniciar la app", es
 * decir, a cerrar y volver a abrir la base de datos Room desde el mismo fichero (sin mantener
 * ninguna instancia en memoria entre medias), tal y como ocurre entre dos ejecuciones reales del
 * programa.
 */
class ManufacturerPreferencesRepositoryImplTest {

    @Test
    fun `los fabricantes deshabilitados persisten tras reabrir la base de datos`() = runBlocking {
        val dbPath = createTempFile(suffix = ".db").toString()

        val firstRunDatabase = openFilamentCacheDatabase(dbPath)
        val firstRunRepository = ManufacturerPreferencesRepositoryImpl(database = firstRunDatabase)

        assertTrue(firstRunRepository.loadDisabledManufacturers().isEmpty(), "Por defecto no hay nada deshabilitado")

        firstRunRepository.saveDisabledManufacturers(setOf("Overture", "Sunlu"))
        firstRunDatabase.close()

        // Simula un reinicio de la app: se abre una instancia nueva de la base de datos Room
        // desde el mismo fichero, sin reutilizar nada en memoria de la ejecución anterior.
        val secondRunDatabase = openFilamentCacheDatabase(dbPath)
        val secondRunRepository = ManufacturerPreferencesRepositoryImpl(database = secondRunDatabase)

        assertEquals(setOf("Overture", "Sunlu"), secondRunRepository.loadDisabledManufacturers())

        // Actualizar el conjunto reemplaza el anterior por completo.
        secondRunRepository.saveDisabledManufacturers(setOf("Bambu Lab"))
        secondRunDatabase.close()

        val thirdRunDatabase = openFilamentCacheDatabase(dbPath)
        val thirdRunRepository = ManufacturerPreferencesRepositoryImpl(database = thirdRunDatabase)
        assertEquals(setOf("Bambu Lab"), thirdRunRepository.loadDisabledManufacturers())
        thirdRunDatabase.close()
    }
}
