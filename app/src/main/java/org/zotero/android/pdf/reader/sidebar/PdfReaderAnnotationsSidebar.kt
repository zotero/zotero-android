package org.zotero.android.pdf.reader.sidebar

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.reader.PdfReaderBottomPanel
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.PdfSidebarSearchBar
import org.zotero.android.pdf.reader.sidebar.rows.PdfReaderAnnotationsSidebarFreeTextRow
import org.zotero.android.pdf.reader.sidebar.rows.PdfReaderAnnotationsSidebarHighlightRow
import org.zotero.android.pdf.reader.sidebar.rows.PdfReaderAnnotationsSidebarImageRow
import org.zotero.android.pdf.reader.sidebar.rows.PdfReaderAnnotationsSidebarInkRow
import org.zotero.android.pdf.reader.sidebar.rows.PdfReaderAnnotationsSidebarNoteRow
import org.zotero.android.pdf.reader.sidebar.rows.PdfReaderAnnotationsSidebarUnderlineRow
import org.zotero.android.pdf.reader.sidebar.sections.PdfReaderAnnotationsSidebarHeaderSection
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun PdfReaderAnnotationsSidebar(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    layoutType: CustomLayoutSize.LayoutType,
    annotationsLazyListState: LazyListState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .windowInsetsPadding(NavigationBarDefaults.windowInsets),
    ) {
        LazyColumn(
            state = annotationsLazyListState,
            verticalArrangement = Arrangement.Absolute.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                PdfSidebarSearchBar(
                    searchValue = viewState.annotationSearchTerm,
                    onSearch = vMInterface::onAnnotationSearch,
                )
            }
            itemsIndexed(
                items = viewState.sortedKeys,
            ) { _, key ->
                val annotation = vMInterface.annotation(key) ?: return@itemsIndexed
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
                            onClick = { vMInterface.selectAnnotation(key) },
                        )
                ) {
                    val annotationColor =
                        Color(annotation.displayColor.toColorInt())
                    val loadPreview = {
                        val preview =
                            vMInterface.annotationPreviewMemoryCache.getBitmap(annotation.key)
                        if (preview == null) {
                            vMInterface.loadAnnotationPreviews(listOf(annotation.key))
                        }
                        preview
                    }

                    PdfReaderAnnotationsSidebarHeaderSection(
                        annotation = annotation,
                        annotationColor = annotationColor,
                        viewState = viewState,
                        vMInterface = vMInterface,
                    )
                    PdfReaderSidebarDivider()

                    when (annotation.type) {
                        AnnotationType.note -> {
                            PdfReaderAnnotationsSidebarNoteRow(
                                annotation = annotation,
                                vMInterface = vMInterface,
                                viewState = viewState,
                            )
                        }

                        AnnotationType.highlight -> {
                            PdfReaderAnnotationsSidebarHighlightRow(
                                annotation = annotation,
                                annotationColor = annotationColor,
                                vMInterface = vMInterface,
                                viewState = viewState,
                            )
                        }

                        AnnotationType.ink -> {
                            PdfReaderAnnotationsSidebarInkRow(
                                vMInterface = vMInterface,
                                viewState = viewState,
                                annotation = annotation,
                                loadPreview = loadPreview,
                            )
                        }

                        AnnotationType.image -> {
                            PdfReaderAnnotationsSidebarImageRow(
                                annotation = annotation,
                                loadPreview = loadPreview,
                                vMInterface = vMInterface,
                                viewState = viewState,
                            )
                        }

                        AnnotationType.text -> {
                            PdfReaderAnnotationsSidebarFreeTextRow(
                                vMInterface = vMInterface,
                                viewState = viewState,
                                annotation = annotation,
                                loadPreview = loadPreview,
                            )
                        }

                        AnnotationType.underline -> {
                            PdfReaderAnnotationsSidebarUnderlineRow(
                                annotation = annotation,
                                annotationColor = annotationColor,
                                vMInterface = vMInterface,
                                viewState = viewState,
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(layoutType.calculateAllItemsBottomPanelHeight()))
            }
        }
        PdfReaderBottomPanel(
            layoutType = layoutType,
            vMInterface = vMInterface,
            viewState = viewState
        )
    }
}
