package es.bancamarch.colorblind3d

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import es.bancamarch.colorblind3d.application.GetAvailableManufacturersUseCase
import es.bancamarch.colorblind3d.application.LoadManufacturerFilterUseCase
import es.bancamarch.colorblind3d.application.LoadThreeMfFileUseCase
import es.bancamarch.colorblind3d.application.MatchFilamentsForColorUseCase
import es.bancamarch.colorblind3d.application.PreloadFilamentCatalogUseCase
import es.bancamarch.colorblind3d.application.RequestMoreAlternativesUseCase
import es.bancamarch.colorblind3d.application.SaveManufacturerFilterUseCase
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.FilamentColorRepositoryImpl
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.ManufacturerPreferencesRepositoryImpl
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.defaultFilamentCacheDatabasePath
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.openFilamentCacheDatabase
import es.bancamarch.colorblind3d.infrastructure.threemf.ThreeMfReaderImpl
import es.bancamarch.colorblind3d.ui.screens.MainScreen
import es.bancamarch.colorblind3d.ui.state.MainViewModel

fun main() = application {
    // Composición manual de dependencias (sin framework de DI): infraestructura -> casos de uso -> UI.
    val threeMfReader = ThreeMfReaderImpl()
    val filamentCacheDatabase = openFilamentCacheDatabase(defaultFilamentCacheDatabasePath())
    val filamentColorRepository = FilamentColorRepositoryImpl(database = filamentCacheDatabase)
    val manufacturerPreferencesRepository = ManufacturerPreferencesRepositoryImpl(database = filamentCacheDatabase)

    val viewModel = MainViewModel(
        loadThreeMfFile = LoadThreeMfFileUseCase(threeMfReader),
        matchFilamentsForColor = MatchFilamentsForColorUseCase(filamentColorRepository),
        requestMoreAlternatives = RequestMoreAlternativesUseCase(filamentColorRepository),
        preloadFilamentCatalog = PreloadFilamentCatalogUseCase(filamentColorRepository),
        getAvailableManufacturers = GetAvailableManufacturersUseCase(filamentColorRepository),
        loadManufacturerFilter = LoadManufacturerFilterUseCase(manufacturerPreferencesRepository),
        saveManufacturerFilter = SaveManufacturerFilterUseCase(manufacturerPreferencesRepository),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Colorblind Friend",
        state = rememberWindowState(size = DpSize(1100.dp, 800.dp)),
    ) {
        MaterialTheme {
            MainScreen(viewModel)
        }
    }
}
