package es.bancamarch.colorblind3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.bancamarch.colorblind3d.ui.components.CatalogPreloadBanner
import es.bancamarch.colorblind3d.ui.components.CatalogPreloadErrorBanner
import es.bancamarch.colorblind3d.ui.components.ManufacturerSelectorDialog
import es.bancamarch.colorblind3d.ui.components.ModelColorSection
import es.bancamarch.colorblind3d.ui.state.ColorMatchState
import es.bancamarch.colorblind3d.ui.state.MainViewModel
import es.bancamarch.colorblind3d.ui.util.pickThreeMfFile

/**
 * Pantalla principal de la aplicación: permite abrir un fichero 3MF y muestra, para cada color
 * detectado, los filamentos reales que mejor coinciden.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Colorblind 3D — colores de modelos 3MF") },
                actions = {
                    IconButton(
                        onClick = viewModel::onShowManufacturerSelector,
                        enabled = uiState.availableManufacturers.isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.FilterAlt, contentDescription = "Filtrar marcas de filamento")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isManufacturerSelectorVisible) {
            ManufacturerSelectorDialog(
                availableManufacturers = uiState.availableManufacturers,
                enabledManufacturers = uiState.enabledManufacturers,
                onToggleManufacturer = viewModel::onToggleManufacturer,
                onSelectAll = viewModel::onSelectAllManufacturers,
                onDeselectAll = viewModel::onDeselectAllManufacturers,
                onDismiss = viewModel::onDismissManufacturerSelector,
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (uiState.isPreloadingCatalog) {
                CatalogPreloadBanner(modifier = Modifier.padding(bottom = 12.dp))
            } else if (uiState.catalogPreloadErrorMessage != null) {
                CatalogPreloadErrorBanner(
                    message = uiState.catalogPreloadErrorMessage ?: "",
                    onRetry = viewModel::preloadCatalog,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    pickThreeMfFile()?.let { viewModel.onFileSelected(it) }
                }) {
                    Text("Abrir fichero 3MF")
                }
                Spacer(modifier = Modifier.padding(start = 8.dp))
                uiState.fileName?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoadingFile -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text("Leyendo el fichero 3MF…")
                }

                uiState.fileErrorMessage != null -> Text(
                    text = uiState.fileErrorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                )

                uiState.colors.isEmpty() -> Text(
                    text = "Abre un fichero .3mf de Bambu Studio para ver los colores que usa y las " +
                        "sugerencias de filamento equivalentes.",
                    style = MaterialTheme.typography.bodyLarge,
                )

                else -> LazyColumn {
                    items(uiState.colors) { colorSample ->
                        ModelColorSection(
                            colorSample = colorSample,
                            matchState = uiState.matchesByColor[colorSample] ?: ColorMatchState(),
                            onRequestMoreAlternatives = { viewModel.onRequestMoreAlternatives(colorSample) },
                            onRetry = { viewModel.onRetryMatchesFor(colorSample) },
                        )
                    }
                }
            }
        }
    }
}
