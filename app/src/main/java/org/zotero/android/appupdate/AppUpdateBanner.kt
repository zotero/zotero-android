package org.zotero.android.appupdate

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun MaybeShowAppUpdateBanner(
    appUpdateBannerPayload: String,
    shouldShowAppUpdateBanner: Boolean,
    onDownloadButtonTapped: () -> Unit,
    onLaterButtonTapped: () -> Unit
) {
    AnimatedContent(targetState = shouldShowAppUpdateBanner, label = "") { showAppUpdateBanner ->
        if (showAppUpdateBanner) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(
                        Strings.app_update_banner_message,
                        appUpdateBannerPayload
                    ),
                    color = CustomTheme.colors.defaultTextColor,
                    style = CustomTheme.typography.info,
                )
                Row(modifier = Modifier.align(Alignment.End)) {
                    NewHeadingTextButton(
                        onClick = onLaterButtonTapped,
                        text = stringResource(id = Strings.app_update_later_button)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    NewHeadingTextButton(
                        onClick = onDownloadButtonTapped,
                        text = stringResource(id = Strings.app_update_download_button)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 16.dp))
                }
                NewDivider()
            }
        }
    }
}