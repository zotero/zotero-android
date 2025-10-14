
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.uicomponents.Strings
import java.util.Locale

@Composable
internal fun PdfAnnotationMoreSizeSelector(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(end = 10.dp),
            text = stringResource(id = Strings.pdf_annotation_popover_line_width),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = viewState.lineWidth,
            onValueChange = { viewModel.onSizeChanged(it) },
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                thumbColor = MaterialTheme.colorScheme.primary,
            ),
            valueRange = 0.5f..25f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = String.format(Locale.getDefault(), "%.1f", viewState.lineWidth),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
