package org.zotero.android.screens.share.bottompanel

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun BoxScope.ShareBottomProgressContainer(
    message: String,
    showActivityIndicator: Boolean
) {

    Row(modifier = Modifier.align(Alignment.Center)) {
        if (showActivityIndicator) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = CustomTheme.colors.secondaryContent,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = CustomTheme.colors.secondaryContent,
        )
    }

}