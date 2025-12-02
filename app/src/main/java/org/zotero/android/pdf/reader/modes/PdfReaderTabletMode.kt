package org.zotero.android.pdf.reader.modes

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.PdfReaderPspdfKitBox
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.sidebar.PdfReaderSidebar
import org.zotero.android.pdf.reader.sidebar.SidebarDivider

@Composable
internal fun PdfReaderTabletMode(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotationsLazyListState: LazyListState,
    thumbnailsLazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
    uri: Uri,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = viewState.showSideBar, transitionSpec = {
            pdfReaderSidebarTransitionSpec()
        }, label = "") { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(330.dp)
                ) {
                    PdfReaderSidebar(
                        vMInterface = vMInterface,
                        viewState = viewState,
                        annotationsLazyListState = annotationsLazyListState,
                        thumbnailsLazyListState = thumbnailsLazyListState,
                        layoutType = layoutType,
                    )
                }
            }
        }
        if (viewState.showSideBar) {
            SidebarDivider(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
            )
        }

        PdfReaderPspdfKitBox(
            uri = uri,
            viewState = viewState,
            vMInterface = vMInterface,
        )
    }
}
