package es.bancamarch.colorblind3d.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.bancamarch.colorblind3d.domain.ColorSample
import es.bancamarch.colorblind3d.ui.state.ColorMatchState

/**
 * Agrupa la muestra de un color del modelo ([ColorSwatchChip]) con la lista de filamentos
 * sugeridos ([FilamentMatchList]) para ese color. Es la unidad de UI que se repite por cada
 * color detectado en el fichero 3MF.
 */
@Composable
fun ModelColorSection(
    colorSample: ColorSample,
    matchState: ColorMatchState,
    onRequestMoreAlternatives: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        ColorSwatchChip(colorSample)
        FilamentMatchList(
            state = matchState,
            onRequestMoreAlternatives = onRequestMoreAlternatives,
            onRetry = onRetry,
            modifier = Modifier.padding(top = 8.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}
