package es.bancamarch.colorblind3d.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Diálogo con la lista de fabricantes de filamento disponibles, cada uno con su casilla de
 * habilitado/deshabilitado (todos habilitados por defecto), para restringir qué marcas se tienen
 * en cuenta al buscar coincidencias de color. Incluye un buscador rápido que filtra la lista
 * visible por nombre (sin afectar a qué marcas están habilitadas).
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ManufacturerSelectorDialog(
    availableManufacturers: List<String>,
    enabledManufacturers: Set<String>,
    onToggleManufacturer: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredManufacturers = remember(availableManufacturers, searchQuery) {
        if (searchQuery.isBlank()) {
            availableManufacturers
        } else {
            availableManufacturers.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marcas de filamento a considerar") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSelectAll) { Text("Marcar todas") }
                    TextButton(onClick = onDeselectAll) { Text("Desmarcar todas") }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    placeholder = { Text("Buscar marca…") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                )
                Text(
                    text = "${enabledManufacturers.size} de ${availableManufacturers.size} marcas habilitadas",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (filteredManufacturers.isEmpty()) {
                    Text(
                        text = "Ninguna marca coincide con «$searchQuery».",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(360.dp)) {
                        items(filteredManufacturers) { manufacturer ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = manufacturer in enabledManufacturers,
                                    onCheckedChange = { onToggleManufacturer(manufacturer) },
                                )
                                Text(manufacturer)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}
