package org.zotero.android.screens.settings.account.sections

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.api.network.CustomResult
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.webdav.data.WebDavError

@Composable
internal fun SettingsAccountFileSyncingErrorMessageItem(
    generalError: CustomResult.GeneralError,
) {
    val errorMessage = WebDavError.message(generalError)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = errorMessage,
            style = CustomTheme.typography.newBody,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
