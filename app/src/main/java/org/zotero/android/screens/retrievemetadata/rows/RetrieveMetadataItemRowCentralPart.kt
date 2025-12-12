package org.zotero.android.screens.retrievemetadata.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
internal fun RetrieveMetadataItemRowCentralPart(
    title:String,
    retrieveMetadataState: RetrieveMetadataState,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (retrieveMetadataState) {
                        is RetrieveMetadataState.failed -> {
                            ErrorMessagePart(retrieveMetadataState.message)
                        }
                        RetrieveMetadataState.fileIsNotPdf -> {
                            //no-op
                        }
                        RetrieveMetadataState.loading -> {
                            LoadingPart()
                        }
                        RetrieveMetadataState.recognizedDataIsEmpty -> {
                            SuccessMessagePart(stringResource(Strings.retrieve_metadata_status_no_results))
                        }
                        is RetrieveMetadataState.success -> {
                            SuccessMessagePart(retrieveMetadataState.recognizedTitle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingPart() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ErrorMessagePart(errorMessage: String) {
    Image(
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
        painter = painterResource(id = Drawables.cancel_20),
        contentDescription = null,
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        modifier = Modifier,
        text = errorMessage,
        color = MaterialTheme.colorScheme.error,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun SuccessMessagePart(successMessage: String) {
    Image(
        colorFilter = ColorFilter.tint(CustomPalette.Green),
        painter = painterResource(id = Drawables.check_circle_20),
        contentDescription = null,
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        text = successMessage,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = CustomPalette.SystemGray,
    )
}