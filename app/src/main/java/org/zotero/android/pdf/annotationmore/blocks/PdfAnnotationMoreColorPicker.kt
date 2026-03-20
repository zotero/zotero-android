
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState

@Composable
internal fun PdfAnnotationMoreColorPicker(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    val selectedColor = viewState.color
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            viewState.colors.forEach { itemColor ->
                PdfAnnotationMoreFilterCircle(
                    hex = itemColor.colorHex,
                    isSelected = itemColor == selectedColor,
                    onClick = { viewModel.onColorSelected(itemColor) })
            }
        }
        if (viewState.isColorLabelsEnabled) {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "${selectedColor?.colorName}(${selectedColor?.colorHex?.uppercase()})",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

    }
}