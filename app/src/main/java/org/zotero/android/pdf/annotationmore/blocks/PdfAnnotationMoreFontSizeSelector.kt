import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.uicomponents.theme.CustomTheme
import java.util.Locale

@Composable
internal fun PdfAnnotationMoreFontSizeSelector(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f", viewState.fontSize),
                color = CustomTheme.colors.defaultTextColor,
                style = CustomTheme.typography.default,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "pt",
                color = CustomTheme.colors.pdfSizePickerColor,
                style = CustomTheme.typography.default,
                fontSize = 16.sp,
            )
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            PdfAnnotationMoreFontSizeChangeButton(text = "-", onClick = viewModel::onFontSizeDecrease)
            Spacer(modifier = Modifier.width(2.dp))
            PdfAnnotationMoreFontSizeChangeButton(text = "+", onClick = viewModel::onFontSizeIncrease)
        }
    }
}
