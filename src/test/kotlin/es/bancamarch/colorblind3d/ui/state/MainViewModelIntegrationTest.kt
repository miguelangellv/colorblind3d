package es.bancamarch.colorblind3d.ui.state

import es.bancamarch.colorblind3d.application.GetAvailableManufacturersUseCase
import es.bancamarch.colorblind3d.application.LoadManufacturerFilterUseCase
import es.bancamarch.colorblind3d.application.LoadThreeMfFileUseCase
import es.bancamarch.colorblind3d.application.MatchFilamentsForColorUseCase
import es.bancamarch.colorblind3d.application.PreloadFilamentCatalogUseCase
import es.bancamarch.colorblind3d.application.RequestMoreAlternativesUseCase
import es.bancamarch.colorblind3d.application.SaveManufacturerFilterUseCase
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.FilamentColorRepositoryImpl
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.ManufacturerPreferencesRepositoryImpl
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.openFilamentCacheDatabase
import es.bancamarch.colorblind3d.infrastructure.threemf.ThreeMfReaderImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Prueba de integración de humo de todo el flujo real (sin red simulada): carga un 3MF real,
 * calcula matches contra la API real de filamentcolors.xyz y pide más alternativas, verificando
 * que el [MainViewModel] termina exponiendo el estado esperado. Se omite si el fichero de
 * ejemplo no está disponible en el entorno (p.ej. en CI, o en otra máquina distinta a la del
 * desarrollador original).
 */
class MainViewModelIntegrationTest {

    @Test
    fun `flujo completo abrir 3MF, ver matches y pedir mas alternativas`() = runBlocking {
        val samplePath = System.getProperty("user.home") + "/Descargas/Ocarina_of_Time_N64_Songs_FINAL.3mf"
        if (!Path(samplePath).exists()) return@runBlocking

        val database = openFilamentCacheDatabase(createTempFile(suffix = ".db").toString())
        val repository = FilamentColorRepositoryImpl(database = database)
        val manufacturerPreferencesRepository = ManufacturerPreferencesRepositoryImpl(database = database)
        val viewModel = MainViewModel(
            loadThreeMfFile = LoadThreeMfFileUseCase(ThreeMfReaderImpl()),
            matchFilamentsForColor = MatchFilamentsForColorUseCase(repository),
            requestMoreAlternatives = RequestMoreAlternativesUseCase(repository),
            preloadFilamentCatalog = PreloadFilamentCatalogUseCase(repository),
            getAvailableManufacturers = GetAvailableManufacturersUseCase(repository),
            loadManufacturerFilter = LoadManufacturerFilterUseCase(manufacturerPreferencesRepository),
            saveManufacturerFilter = SaveManufacturerFilterUseCase(manufacturerPreferencesRepository),
        )

        viewModel.onFileSelected(samplePath)

        val loadedState = waitUntil(viewModel, timeoutMillis = 90_000) { it.colors.isNotEmpty() }
        println("Colores cargados: ${loadedState.colors.map { it.label + " #" + it.rgb.hex }}")
        assertTrue(loadedState.colors.size >= 4, "Se esperaban al menos 4 colores en el modelo multicolor")

        val firstColor = loadedState.colors.first()
        val withMatches = waitUntil(viewModel, timeoutMillis = 90_000) {
            (it.matchesByColor[firstColor]?.matches?.size ?: 0) >= 5
        }
        val initialMatches = withMatches.matchesByColor.getValue(firstColor).matches
        println("Matches iniciales para ${firstColor.label}: ${initialMatches.map { it.swatch.colorName }}")
        assertTrue(initialMatches.size == 5)

        viewModel.onRequestMoreAlternatives(firstColor)
        val withMoreMatches = waitUntil(viewModel, timeoutMillis = 90_000) {
            (it.matchesByColor[firstColor]?.matches?.size ?: 0) > 5
        }
        val allMatches = withMoreMatches.matchesByColor.getValue(firstColor).matches
        println("Matches tras pedir más alternativas: ${allMatches.map { it.swatch.colorName }}")
        assertTrue(allMatches.size == 10)
        assertTrue(allMatches.map { it.swatch.id }.distinct().size == 10, "No debería haber duplicados")
    }

    private suspend fun waitUntil(
        viewModel: MainViewModel,
        timeoutMillis: Long,
        condition: (MainUiState) -> Boolean,
    ): MainUiState {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            val state = viewModel.uiState.value
            if (condition(state)) return state
            delay(100)
        }
        error("Tiempo de espera agotado. Último estado: ${viewModel.uiState.value}")
    }
}
