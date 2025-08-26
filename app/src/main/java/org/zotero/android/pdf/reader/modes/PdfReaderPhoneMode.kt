package org.zotero.android.pdf.reader.modes

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.PdfReaderPspdfKitBox
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchScreen
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewModel
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewState
import org.zotero.android.pdf.reader.sidebar.PdfReaderSidebar

@Composable
internal fun PdfReaderPhoneMode(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    pdfReaderSearchViewState: PdfReaderSearchViewState,
    pdfReaderSearchViewModel: PdfReaderSearchViewModel,
    annotationsLazyListState: LazyListState,
    thumbnailsLazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
    uri: Uri,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PdfReaderPspdfKitBox(
            uri = uri,
            viewState = viewState,
            vMInterface = vMInterface
        )
        AnimatedContent(
            targetState = viewState.showSideBar,
            transitionSpec = {
                pdfReaderSidebarTransitionSpec()
            }, label = ""
        ) { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                //Prevent tap to be propagated to composables behind this screen.
                            }
                        }) {
                    PdfReaderSidebar(
                        viewState = viewState,
                        vMInterface = vMInterface,
                        annotationsLazyListState = annotationsLazyListState,
                        thumbnailsLazyListState = thumbnailsLazyListState,
                        layoutType = layoutType,
                    )
                }
            }
        }
        AnimatedContent(targetState = viewState.showPdfSearch, transitionSpec = {
            pdfReaderPdfSearchTransitionSpec()
        }, label = "") { showScreen ->
            if (showScreen) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    PdfReaderSearchScreen(
                        onBack = vMInterface::hidePdfSearch,
                        viewModel = pdfReaderSearchViewModel,
                        viewState = pdfReaderSearchViewState,
                    )
                }
            }
        }
    }
}
