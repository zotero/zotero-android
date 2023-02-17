package org.zotero.android.screens.mediaviewer.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.HeadingTextButton
import org.zotero.android.uicomponents.topbar.NoIconTopBar

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

@Composable
private fun ImageViewerTopBar(
    title: String,
    onDoneClicked: () -> Unit,
) {
    NoIconTopBar(
        title = title,
        actions = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onDoneClicked,
                text = stringResource(Strings.done)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    )

}