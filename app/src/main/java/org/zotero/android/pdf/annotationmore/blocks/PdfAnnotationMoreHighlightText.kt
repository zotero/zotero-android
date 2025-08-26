
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun PdfAnnotationMoreHighlightText(
    annotationColor: Color,
    viewState: PdfAnnotationMoreViewState,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(3.dp)
                .fillMaxHeight()
                .background(annotationColor)
        )

        CustomTextField(
            modifier = Modifier.padding(start = 27.dp, end = 16.dp),
            value = viewState.highlightText,
            hint = "",
            ignoreTabsAndCaretReturns = false,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
