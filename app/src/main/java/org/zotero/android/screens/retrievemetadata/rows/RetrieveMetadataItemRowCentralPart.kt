package org.zotero.android.screens.retrievemetadata.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

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
                    color = CustomTheme.colors.allItemsRowTitleColor,
                    style = CustomTheme.typography.newHeadline
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
    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        backgroundColor = Color(0x29787880),
        color = Color(0xFF1A88FF),
    )
}

@Composable
private fun RowScope.ErrorMessagePart(errorMessage: String) {
    Image(
        modifier = Modifier.size(16.dp),
        painter = painterResource(id = Drawables.failure),
        contentDescription = null,
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        modifier = Modifier,
        text = errorMessage,
        color = CustomPalette.ErrorRed,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = CustomTheme.typography.newBody,
    )
}

@Composable
private fun RowScope.SuccessMessagePart(successMessage: String) {
    Image(
        modifier = Modifier.size(16.dp),
        painter = painterResource(id = Drawables.success),
        contentDescription = null,
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        text = successMessage,
        style = CustomTheme.typography.newBody,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = CustomPalette.SystemGray,
    )
}