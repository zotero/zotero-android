package org.zotero.android.screens.mediaviewer.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.uicomponents.CustomScaffold

@Composable
internal fun ImageViewerScreen(onBack: () -> Unit) {
    val uri = ScreenArguments.imageViewerArgs.uri
    val title = ScreenArguments.imageViewerArgs.title

    CustomScaffold(
        topBar = {
            ImageViewerTopBar(title = title, onDoneClicked = onBack)
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current).data(uri).build()
                ),
                contentDescription = title,
            )
        }
    }
}