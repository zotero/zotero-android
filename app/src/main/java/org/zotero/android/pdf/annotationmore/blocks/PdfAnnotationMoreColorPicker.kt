
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState

@Composable
internal fun PdfAnnotationMoreColorPicker(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val selectedColor = viewState.color
        viewState.colors.forEach { listColorHex ->
            PdfAnnotationMoreFilterCircle(
                hex = listColorHex,
                isSelected = listColorHex == selectedColor,
                onClick = { viewModel.onColorSelected(listColorHex) })
        }
    }

}