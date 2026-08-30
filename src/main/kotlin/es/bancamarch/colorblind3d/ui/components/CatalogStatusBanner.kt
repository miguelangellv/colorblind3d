package es.bancamarch.colorblind3d.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Aviso mostrado mientras se precarga (descarga y cachea) el catálogo de filamentos de
 * filamentcolors.xyz al arrancar la aplicación, para que el usuario sepa por qué las
 * coincidencias podrían tardar un poco la primera vez.
 */
@Composable
fun CatalogPreloadBanner(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(2.dp))
            Text(
                text = "Descargando catálogo de colores de filamentcolors.xyz…",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Aviso mostrado si la precarga del catálogo de filamentos falla, con botón para reintentar. */
@Composable
fun CatalogPreloadErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}
