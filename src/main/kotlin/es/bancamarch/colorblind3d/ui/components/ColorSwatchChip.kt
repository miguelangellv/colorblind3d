package es.bancamarch.colorblind3d.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import es.bancamarch.colorblind3d.domain.ColorSample

/**
 * Muestra en texto un color detectado en el modelo: una muestra visual del color junto con su
 * etiqueta y su código hexadecimal, para que una persona daltónica pueda identificarlo por
 * nombre/código en lugar de solo por el tono.
 */
@Composable
fun ColorSwatchChip(colorSample: ColorSample, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = Color(colorSample.rgb.red, colorSample.rgb.green, colorSample.rgb.blue),
                    shape = RoundedCornerShape(6.dp),
                )
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(6.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = colorSample.label, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "#${colorSample.rgb.hex}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
