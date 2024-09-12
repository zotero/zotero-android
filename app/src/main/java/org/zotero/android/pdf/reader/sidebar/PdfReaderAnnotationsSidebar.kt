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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.reader.PdfReaderBottomPanel
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.PdfSidebarSearchBar
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

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
            .background(CustomTheme.colors.pdfAnnotationsFormBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = layoutType.calculateAllItemsBottomPanelHeight())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            PdfSidebarSearchBar(
                searchValue = viewState.annotationSearchTerm,
                onSearch = vMInterface::onAnnotationSearch,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                state = annotationsLazyListState,
                verticalArrangement = Arrangement.Absolute.spacedBy(13.dp),
            ) {
                itemsIndexed(
                    items = viewState.sortedKeys,
                ) { _, key ->
                    val annotation = vMInterface.annotation(key) ?: return@itemsIndexed
                    val isSelected = viewState.isAnnotationSelected(annotation.key)
                    val horizontalPadding = if (isSelected) 13.dp else 16.dp
                    var rowModifier: Modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .clip(shape = RoundedCornerShape(10.dp))
                        .background(CustomTheme.colors.pdfAnnotationsItemBackground)

                    if (isSelected) {
                        rowModifier = rowModifier.border(
                            width = 3.dp,
                            color = CustomTheme.colors.zoteroDefaultBlue,
                            shape = RoundedCornerShape(10.dp)
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
                            Color(android.graphics.Color.parseColor(annotation.displayColor))
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
                        SidebarDivider()

                        when (annotation.type) {
                            AnnotationType.note -> PdfReaderAnnotationsSidebarNoteRow(
                                annotation = annotation,
                                vMInterface = vMInterface,
                                viewState = viewState,
                            )

                            AnnotationType.highlight -> PdfReaderAnnotationsSidebarHighlightRow(
                                annotation = annotation,
                                annotationColor = annotationColor,
                                vMInterface = vMInterface,
                                viewState = viewState,
                            )

                            AnnotationType.ink -> PdfReaderAnnotationsSidebarInkRow(
                                vMInterface = vMInterface,
                                viewState = viewState,
                                annotation = annotation,
                                loadPreview = loadPreview,
                            )

                            AnnotationType.image -> PdfReaderAnnotationsSidebarImageRow(
                                annotation = annotation,
                                loadPreview = loadPreview,
                                vMInterface = vMInterface,
                                viewState = viewState,
                            )

                           else -> {
                               //TODO freeText and underline sidebar rows
                           }
                        }
                    }
                }
            }
        }
        PdfReaderBottomPanel(
            layoutType = layoutType,
            vMInterface = vMInterface,
            viewState = viewState
        )
    }
}
