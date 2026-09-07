package org.zotero.android.screens.reader.sidebar.annotations.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.reader.data.ReaderAnnotation

@Composable
internal fun ReaderAnnotationsSidebarFreeTextContentSection(
    annotation: ReaderAnnotation,
) {
    if (annotation.comment.isBlank()) {
        return
    }
    Text(
        modifier = Modifier
            .sectionHorizontalPadding()
            .padding(top = 8.dp)
            .padding(bottom = 8.dp),
        text = annotation.comment,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
    )
}
