package org.zotero.android.pdf.reader.sidebar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import org.zotero.android.androidx.content.pxToDp
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfReaderThumbnailsSidebar(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    thumbnailsLazyListState: LazyListState,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = thumbnailsLazyListState,
            verticalArrangement = Arrangement.Absolute.spacedBy(16.dp),
        ) {
            itemsIndexed(
                items = viewState.thumbnailRows
            ) { _, row ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isSelected = viewState.isThumbnailSelected(row)
                    val horizontalPadding = if (isSelected) 13.dp else 16.dp
                    var rowModifier: Modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .clip(shape = RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)

                    if (isSelected) {
                        rowModifier = rowModifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Column(
                        modifier = rowModifier
                            .safeClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    vMInterface.selectThumbnail(row)
                                },
                            )
                    ) {
                        val loadPreview =  {
                            val preview =
                                vMInterface.thumbnailPreviewMemoryCache.getBitmap(row.pageIndex)
                            if (preview == null) {
                                vMInterface.loadThumbnailPreviews(row.pageIndex)
                            }
                            preview
                        }
                        val cachedBitmap = loadPreview()
                        if (cachedBitmap == null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth(fraction = 0.7f)
                                    .height(vMInterface.annotationMaxSideSize.pxToDp()),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = CustomTheme.colors.secondaryContent,
                                    strokeWidth = 2.dp,
                                )
                            }
                        } else {
                            Image(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = 0.7f)
                                    .height(vMInterface.annotationMaxSideSize.pxToDp()),
                                bitmap = cachedBitmap!!.asImageBitmap(),
                                contentDescription = null,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = row.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
            }
        }
    }
}
