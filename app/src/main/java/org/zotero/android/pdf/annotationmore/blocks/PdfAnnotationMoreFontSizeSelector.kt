import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import java.util.Locale

@Composable
internal fun PdfAnnotationMoreFontSizeSelector(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f", viewState.fontSize),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "pt",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            PdfAnnotationMoreFontSizeChangeButton(text = "-", onClick = viewModel::onFontSizeDecrease)
            Spacer(modifier = Modifier.width(2.dp))
            PdfAnnotationMoreFontSizeChangeButton(text = "+", onClick = viewModel::onFontSizeIncrease)
        }
    }
}
