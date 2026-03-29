package org.zotero.android.screens.htmlepub.annotation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.R
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.screens.htmlepub.annotation.sections.HtmlEpubAnnotationCommentSection
import org.zotero.android.screens.htmlepub.annotation.sections.HtmlEpubAnnotationTagsSection
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun HtmlEpubAnnotationHeaderRow(
    annotation: HtmlEpubAnnotation,
    annotationColor: Color,
    onBack: () -> Unit,
) {
    val title = stringResource(R.string.page) + " " + annotation.pageLabel
    val icon = when (annotation.type) {
        AnnotationType.note -> Drawables.annotate_note
        AnnotationType.highlight -> Drawables.annotate_highlight
        AnnotationType.image -> Drawables.annotate_area
        AnnotationType.ink -> Drawables.annotate_ink
        AnnotationType.underline -> Drawables.annotate_underline
        AnnotationType.text -> Drawables.annotate_text
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(annotationColor),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            FilledTonalButton(
                onClick = onBack,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = stringResource(Strings.done),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}


@Composable
internal fun HtmlEpubAnnotationHighlightRow(
    annotation: HtmlEpubAnnotation,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
    colors: List<String>,
    onColorSelected: (color: String) -> Unit,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit
) {
    HtmlEpubAnnotationCommentSection(
        annotation = annotation,
        commentFocusText = commentFocusText,
        onCommentTextChange = onCommentTextChange,
    )
        NewSettingsDivider()
        HtmlEpubAnnotationColorPicker(
            colors = colors,
            onColorSelected = onColorSelected,
            selectedColor = selectedColor,
        )
        NewSettingsDivider()
        HtmlEpubAnnotationTagsSection(
            tags = tags,
            onTagsClicked = onTagsClicked,
        )
}

@Composable
internal fun HtmlEpubAnnotationNoteRow(
    annotation: HtmlEpubAnnotation,
    colors: List<String>,
    onColorSelected: (color: String) -> Unit,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
) {
    HtmlEpubAnnotationCommentSection(
        annotation = annotation,
        commentFocusText = commentFocusText,
        onCommentTextChange = onCommentTextChange,
    )

        NewSettingsDivider()
        HtmlEpubAnnotationColorPicker(
            colors = colors,
            onColorSelected = onColorSelected,
            selectedColor = selectedColor
        )
        NewSettingsDivider()
        HtmlEpubAnnotationTagsSection(
            tags = tags,
            onTagsClicked = onTagsClicked,
        )
}


@Composable
internal fun HtmlEpubAnnotationUnderlineRow(
    annotation: HtmlEpubAnnotation,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
    colors: List<String>,
    onColorSelected: (color: String) -> Unit,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit
) {
    HtmlEpubAnnotationCommentSection(
        annotation = annotation,
        commentFocusText = commentFocusText,
        onCommentTextChange = onCommentTextChange,
    )

        NewSettingsDivider()
        HtmlEpubAnnotationColorPicker(
            colors = colors,
            onColorSelected = onColorSelected,
            selectedColor = selectedColor
        )
        NewSettingsDivider()
        HtmlEpubAnnotationTagsSection(
            tags = tags,
            onTagsClicked = onTagsClicked,
        )
}

