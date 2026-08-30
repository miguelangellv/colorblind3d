package es.bancamarch.colorblind3d.ui.state

import es.bancamarch.colorblind3d.application.GetAvailableManufacturersUseCase
import es.bancamarch.colorblind3d.application.LoadManufacturerFilterUseCase
import es.bancamarch.colorblind3d.application.LoadThreeMfFileUseCase
import es.bancamarch.colorblind3d.application.MatchFilamentsForColorUseCase
import es.bancamarch.colorblind3d.application.PreloadFilamentCatalogUseCase
import es.bancamarch.colorblind3d.application.RequestMoreAlternativesUseCase
import es.bancamarch.colorblind3d.application.SaveManufacturerFilterUseCase
import es.bancamarch.colorblind3d.domain.ColorSample
import es.bancamarch.colorblind3d.domain.ThreeMfParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado y lógica de presentación de la pantalla principal. Orquesta los casos de uso de
 * `application` y expone un [StateFlow] inmutable para que la UI de Compose se limite a
 * renderizar el estado.
 */
class MainViewModel(
    private val loadThreeMfFile: LoadThreeMfFileUseCase,
    private val matchFilamentsForColor: MatchFilamentsForColorUseCase,
    private val requestMoreAlternatives: RequestMoreAlternativesUseCase,
    private val preloadFilamentCatalog: PreloadFilamentCatalogUseCase,
    private val getAvailableManufacturers: GetAvailableManufacturersUseCase,
    private val loadManufacturerFilter: LoadManufacturerFilterUseCase,
    private val saveManufacturerFilter: SaveManufacturerFilterUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        preloadCatalog()
    }

    /**
     * Descarga (o recupera de cache) el catálogo de filamentcolors.xyz nada más arrancar la app,
     * para que abrir un fichero 3MF no tenga que esperar a esa descarga. El resultado se refleja
     * en [MainUiState.isPreloadingCatalog] para mostrar un aviso mientras dure. También carga la
     * lista de fabricantes disponibles y restaura qué marcas dejó deshabilitadas el usuario la
     * última vez (si no hay nada guardado, quedan todas habilitadas).
     */
    fun preloadCatalog() {
        _uiState.update { it.copy(isPreloadingCatalog = true, catalogPreloadErrorMessage = null) }
        scope.launch {
            try {
                preloadFilamentCatalog()
                val manufacturers = getAvailableManufacturers()
                val disabled = loadManufacturerFilter()
                _uiState.update {
                    it.copy(
                        isPreloadingCatalog = false,
                        availableManufacturers = manufacturers,
                        enabledManufacturers = manufacturers.toSet() - disabled,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPreloadingCatalog = false,
                        catalogPreloadErrorMessage = "No se ha podido descargar el catálogo de colores: ${e.message}",
                    )
                }
            }
        }
    }

    fun onShowManufacturerSelector() {
        _uiState.update { it.copy(isManufacturerSelectorVisible = true) }
    }

    fun onDismissManufacturerSelector() {
        _uiState.update { it.copy(isManufacturerSelectorVisible = false) }
    }

    fun onToggleManufacturer(manufacturer: String) {
        _uiState.update { state ->
            val enabled = state.enabledManufacturers
            val updated = if (manufacturer in enabled) enabled - manufacturer else enabled + manufacturer
            state.copy(enabledManufacturers = updated)
        }
        persistManufacturerFilter()
        recalculateAllMatches()
    }

    fun onSelectAllManufacturers() {
        _uiState.update { it.copy(enabledManufacturers = it.availableManufacturers.toSet()) }
        persistManufacturerFilter()
        recalculateAllMatches()
    }

    fun onDeselectAllManufacturers() {
        _uiState.update { it.copy(enabledManufacturers = emptySet()) }
        persistManufacturerFilter()
        recalculateAllMatches()
    }

    /** Guarda en la cache local qué fabricantes han quedado deshabilitados, para recordarlo entre reinicios. */
    private fun persistManufacturerFilter() {
        val state = _uiState.value
        val disabled = state.availableManufacturers.toSet() - state.enabledManufacturers
        scope.launch { saveManufacturerFilter(disabled) }
    }

    /** Vuelve a calcular los matches (top N iniciales) de todos los colores ya cargados, tras un cambio de filtro de marcas. */
    private fun recalculateAllMatches() {
        val colors = _uiState.value.colors
        scope.launch { colors.forEach { color -> loadMatchesFor(color) } }
    }

    fun onFileSelected(filePath: String) {
        scope.launch {
            _uiState.update {
                it.copy(
                    isLoadingFile = true,
                    fileErrorMessage = null,
                    fileName = null,
                    colors = emptyList(),
                    matchesByColor = emptyMap(),
                )
            }
            try {
                val document = loadThreeMfFile(filePath)
                _uiState.update {
                    it.copy(
                        isLoadingFile = false,
                        fileName = document.fileName,
                        colors = document.colors,
                        matchesByColor = emptyMap(),
                    )
                }
                document.colors.forEach { color -> loadMatchesFor(color) }
            } catch (e: ThreeMfParsingException) {
                _uiState.update {
                    it.copy(isLoadingFile = false, fileErrorMessage = e.message ?: "Error al leer el fichero 3MF")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingFile = false, fileErrorMessage = "Error inesperado: ${e.message}")
                }
            }
        }
    }

    fun onRequestMoreAlternatives(color: ColorSample) {
        val currentState = _uiState.value.matchesByColor[color] ?: return
        if (currentState.isLoadingMore) return

        updateColorMatchState(color) { it.copy(isLoadingMore = true, errorMessage = null) }
        scope.launch {
            try {
                val more = requestMoreAlternatives(
                    color,
                    alreadyShownCount = currentState.matches.size,
                    allowedManufacturers = selectedManufacturersOrNull(),
                )
                updateColorMatchState(color) {
                    it.copy(
                        matches = it.matches + more,
                        isLoadingMore = false,
                        hasMoreAlternatives = more.isNotEmpty(),
                    )
                }
            } catch (e: Exception) {
                updateColorMatchState(color) {
                    it.copy(isLoadingMore = false, errorMessage = "No se han podido cargar más alternativas: ${e.message}")
                }
            }
        }
    }

    fun onRetryMatchesFor(color: ColorSample) {
        scope.launch { loadMatchesFor(color) }
    }

    private suspend fun loadMatchesFor(color: ColorSample) {
        updateColorMatchState(color) { it.copy(isLoadingMore = true, errorMessage = null) }
        try {
            val matches = matchFilamentsForColor(color, allowedManufacturers = selectedManufacturersOrNull())
            updateColorMatchState(color) {
                it.copy(matches = matches, isLoadingMore = false, hasMoreAlternatives = matches.isNotEmpty())
            }
        } catch (e: Exception) {
            updateColorMatchState(color) {
                it.copy(isLoadingMore = false, errorMessage = "No se han podido cargar filamentos: ${e.message}")
            }
        }
    }

    /** `null` cuando todas las marcas disponibles están habilitadas, para no filtrar innecesariamente. */
    private fun selectedManufacturersOrNull(): Set<String>? {
        val state = _uiState.value
        return if (state.enabledManufacturers.size >= state.availableManufacturers.size) null else state.enabledManufacturers
    }

    private fun updateColorMatchState(color: ColorSample, transform: (ColorMatchState) -> ColorMatchState) {
        _uiState.update { state ->
            val current = state.matchesByColor[color] ?: ColorMatchState()
            state.copy(matchesByColor = state.matchesByColor + (color to transform(current)))
        }
    }
}
