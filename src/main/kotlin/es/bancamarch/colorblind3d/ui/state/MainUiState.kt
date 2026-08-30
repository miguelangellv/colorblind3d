package es.bancamarch.colorblind3d.ui.state

import es.bancamarch.colorblind3d.domain.ColorSample
import es.bancamarch.colorblind3d.domain.FilamentMatch

/** Estado de las coincidencias de filamento calculadas para un color concreto del modelo. */
data class ColorMatchState(
    val matches: List<FilamentMatch> = emptyList(),
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMoreAlternatives: Boolean = true,
)

/** Estado completo de la pantalla principal, inmutable y expuesto vía StateFlow. */
data class MainUiState(
    val isLoadingFile: Boolean = false,
    val fileErrorMessage: String? = null,
    val fileName: String? = null,
    val colors: List<ColorSample> = emptyList(),
    val matchesByColor: Map<ColorSample, ColorMatchState> = emptyMap(),
    val isPreloadingCatalog: Boolean = true,
    val catalogPreloadErrorMessage: String? = null,
    val availableManufacturers: List<String> = emptyList(),
    val enabledManufacturers: Set<String> = emptySet(),
    val isManufacturerSelectorVisible: Boolean = false,
)
