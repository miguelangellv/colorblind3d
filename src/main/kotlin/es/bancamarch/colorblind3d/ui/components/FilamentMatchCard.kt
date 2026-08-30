package es.bancamarch.colorblind3d.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import es.bancamarch.colorblind3d.domain.FilamentMatch

/**
 * Tarjeta con la imagen y el nombre de un filamento candidato para que el usuario pueda comparar
 * visualmente y decidir cuál se ajusta mejor a lo que necesita imprimir.
 */
@Composable
fun FilamentMatchCard(match: FilamentMatch, modifier: Modifier = Modifier) {
    val swatch = match.swatch
    Card(modifier = modifier.width(160.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFEFEFEF), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = swatch.imageFrontUrl ?: swatch.cardImageUrl
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Muestra de ${swatch.colorName}",
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(swatch.hex.toColorInt()), RoundedCornerShape(6.dp)),
                    )
                }
            }
            Text(
                text = swatch.colorName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(text = swatch.manufacturerName, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${swatch.filamentType} · #${swatch.hex}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Distancia de color: %.1f".format(match.distance),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun String.toColorInt(): Long {
    val clean = removePrefix("#")
    return ("FF$clean").toLong(16)
}
