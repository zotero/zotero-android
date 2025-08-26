import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun PdfAnnotationMoreFontSizeChangeButton(text: String, onClick: (() -> Unit)) {
    val roundCornerShape = RoundedCornerShape(size = 8.dp)

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = roundCornerShape
            )
            .clip(roundCornerShape)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 22.sp,
        )
    }
}
