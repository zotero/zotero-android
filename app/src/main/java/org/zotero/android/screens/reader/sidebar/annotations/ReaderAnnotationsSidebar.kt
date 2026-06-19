package org.zotero.android.screens.reader.sidebar.annotations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.screens.reader.ReaderBottomPanel
import org.zotero.android.screens.reader.ReaderSidebarSearchBar
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.sidebar.ReaderSidebarDivider
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarHeaderSection
import org.zotero.android.screens.reader.sidebar.rows.ReaderAnnotationsSidebarFreeTextRow
import org.zotero.android.screens.reader.sidebar.rows.ReaderAnnotationsSidebarHighlightRow
import org.zotero.android.screens.reader.sidebar.rows.ReaderAnnotationsSidebarImageRow
import org.zotero.android.screens.reader.sidebar.rows.ReaderAnnotationsSidebarInkRow
import org.zotero.android.screens.reader.sidebar.rows.ReaderAnnotationsSidebarNoteRow
import org.zotero.android.screens.reader.sidebar.rows.ReaderAnnotationsSidebarUnderlineRow
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun ReaderAnnotationsSidebar(
    viewModel: ReaderViewModel,
    viewState: ReaderViewState,
    annotationsLazyListState: LazyListState,
    annotationMaxSideSize: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        LazyColumn(
            state = annotationsLazyListState,
            verticalArrangement = Arrangement.Absolute.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ReaderSidebarSearchBar(
                    searchValue = viewState.annotationSearchTerm,
                    onSearch = viewModel::onAnnotationSearch,
                )
            }
            itemsIndexed(
                items = viewState.sortedKeys,
            ) { _, key ->
                val annotation = viewModel.annotation(key) ?: return@itemsIndexed
                val isSelected = viewState.isAnnotationSelected(annotation.key)
                val roundedCornerShape = RoundedCornerShape(14.dp)
                var rowModifier: Modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(shape = roundedCornerShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)

                if (isSelected) {
                    rowModifier = rowModifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = roundedCornerShape
                    )
                }

                Column(
                    modifier = rowModifier
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.selectAnnotation(key) },
                        )
                ) {
                    val annotationColor =
                        Color(annotation.displayColor.toColorInt())

                    ReaderAnnotationsSidebarHeaderSection(
                        annotation = annotation,
                        annotationColor = annotationColor,
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                    ReaderSidebarDivider()

                    val cachedBitmap = viewState.annotationsBitmapCache[key]
                    when (annotation.type) {
                        AnnotationType.note -> {
                            ReaderAnnotationsSidebarNoteRow(
                                annotation = annotation,
                                viewModel = viewModel,
                                viewState = viewState,
                            )
                        }

                        AnnotationType.highlight -> {
                            ReaderAnnotationsSidebarHighlightRow(
                                annotation = annotation,
                                annotationColor = annotationColor,
                                viewModel = viewModel,
                                viewState = viewState,
                            )
                        }

                        AnnotationType.underline -> {
                            ReaderAnnotationsSidebarUnderlineRow(
                                annotation = annotation,
                                annotationColor = annotationColor,
                                viewModel = viewModel,
                                viewState = viewState,
                            )
                        }

                        AnnotationType.image -> {
                            ReaderAnnotationsSidebarImageRow(
                                annotation = annotation,
                                viewModel = viewModel,
                                viewState = viewState,
                                annotationMaxSideSize = annotationMaxSideSize,
                                cachedBitmap = cachedBitmap,
                            )
                        }
                        AnnotationType.ink -> {
                            ReaderAnnotationsSidebarInkRow(
                                annotation = annotation,
                                viewModel = viewModel,
                                viewState = viewState,
                                annotationMaxSideSize = annotationMaxSideSize,
                                cachedBitmap = cachedBitmap,
                            )
                        }
                        AnnotationType.text -> {
                            ReaderAnnotationsSidebarFreeTextRow(
                                annotation = annotation,
                                viewModel = viewModel,
                                viewState = viewState,
                                annotationMaxSideSize = annotationMaxSideSize,
                                cachedBitmap = cachedBitmap,
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
        ReaderBottomPanel(
            viewModel = viewModel,
            viewState = viewState
        )
    }
}
