package org.zotero.android.screens.share

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ShareScreenTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onCancelClicked: () -> Unit,
    onSave: () -> Unit,
    isLeftButtonEnabled: Boolean,
    isRightButtonEnabled: Boolean,
    isLoading: Boolean,
    attachmentError: AttachmentState.Error?,
) {
    when (attachmentError) {
        is AttachmentState.Error.quotaLimit, is AttachmentState.Error.webDavFailure, is AttachmentState.Error.apiFailure -> {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                },
                navigationIcon = {
                },
                actions = {
                    FilledTonalButton(
                        onClick = { onCancelClicked() },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            text = stringResource(Strings.done),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                })
        }

        else -> {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                },
                navigationIcon = {
                    val containerColor = if (isLeftButtonEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        CustomTheme.colors.disabledContent
                    }

                    IconButton(onClick = onCancelClicked, enabled = isLeftButtonEnabled) {
                        Icon(
                            painter = painterResource(Drawables.arrow_back_24dp),
                            contentDescription = null,
                            tint = containerColor,
                        )
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        val containerColor = if (isLeftButtonEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            CustomTheme.colors.disabledContent
                        }

                        FilledTonalButton(
                            onClick = { onSave() }, enabled = isRightButtonEnabled,
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = containerColor),
                        ) {
                            Text(
                                text = stringResource(Strings.shareext_save),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                })

        }
    }

}