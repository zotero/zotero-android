package org.zotero.android.screens.allitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeStringResource

// Shown above the bottom panel while attachments are downloaded in a batch. It changes only when an
// attachment finishes and has no indeterminate animation, so it doesn't keep redrawing e-ink screens
@Composable
internal fun AllItemsDownloadProgress(batchState: AttachmentDownloader.BatchState?) {
    if (batchState == null) {
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BottomAppBarDefaults.containerColor)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        val text = when (batchState) {
            AttachmentDownloader.BatchState.Preparing -> {
                safeStringResource(Strings.all_items_preparing_downloads)
            }
            is AttachmentDownloader.BatchState.Downloading -> {
                safeStringResource(Strings.items_toolbar_downloaded, batchState.downloaded, batchState.total)
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (batchState is AttachmentDownloader.BatchState.Downloading) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { batchState.downloaded / batchState.total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
