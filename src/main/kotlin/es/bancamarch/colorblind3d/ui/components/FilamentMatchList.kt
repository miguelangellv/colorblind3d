package es.bancamarch.colorblind3d.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.bancamarch.colorblind3d.domain.FilamentMatch
import es.bancamarch.colorblind3d.ui.state.ColorMatchState

/**
 * Grid reutilizable de [FilamentMatchCard] para un color: las tarjetas se distribuyen en
 * horizontal y saltan a una nueva fila automáticamente cuando no caben más en el ancho
 * disponible, con botón para pedir más alternativas y estados de carga/error.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun FilamentMatchList(
    state: ColorMatchState,
    onRequestMoreAlternatives: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.matches.forEach { match: FilamentMatch ->
            FilamentMatchCard(match)
        }
        Row(modifier = Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                state.isLoadingMore -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                state.errorMessage != null -> OutlinedButton(onClick = onRetry) { Text("Reintentar") }
                state.hasMoreAlternatives -> OutlinedButton(onClick = onRequestMoreAlternatives) {
                    Text("Más alternativas")
                }
            }
        }
    }
    state.errorMessage?.let {
        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
