package org.zotero.android.uicomponents.topbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CancelSaveTitleTopBar(
    title: String? = null,
    onCancel: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    isSaveButtonEnabled: Boolean = true,
    backgroundColor: Color = CustomTheme.colors.surface,
) {
    CenterAlignedTopAppBar(
        title = {
            if (title != null) {
                Text(
                    text = title,
                    color = CustomTheme.colors.primaryContent,
                    style = CustomTheme.typography.h2
                )
            }
        },
        navigationIcon = {
            Row {
                Spacer(modifier = Modifier.width(4.dp))
                if (onCancel != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    HeadingTextButton(
                        onClick = onCancel,
                        text = stringResource(Strings.cancel),
                    )
                }
                if (onClose != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    HeadingTextButton(
                        onClick = onClose,
                        text = stringResource(Strings.close),
                    )
                }
                if (onBack != null) {
                    Spacer(modifier = Modifier.width(1.dp))
                    HeadingTextButton(
                        onClick = onBack,
                        text = stringResource(Strings.back),
                    )
                }
            }
        },
        actions = {
            var isInitialPaddingUsed = false
            val calculatePadding: () -> Dp = {
                if (isInitialPaddingUsed) {
                    8.dp
                } else {
                    isInitialPaddingUsed = true
                    4.dp
                }
            }
            if (onAdd != null) {
                HeadingTextButton(
                    onClick = onAdd,
                    isEnabled = isSaveButtonEnabled,
                    text = stringResource(Strings.add),
                )
                Spacer(modifier = Modifier.width(calculatePadding()))
            }
            if (onSave != null) {
                HeadingTextButton(
                    onClick = onSave,
                    isEnabled = isSaveButtonEnabled,
                    text = stringResource(Strings.save),
                )
                Spacer(modifier = Modifier.width(calculatePadding()))
            }
            if (onDone != null) {
                HeadingTextButton(
                    onClick = onDone,
                    text = stringResource(Strings.done),
                )
                Spacer(modifier = Modifier.width(calculatePadding()))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor),
    )
}
