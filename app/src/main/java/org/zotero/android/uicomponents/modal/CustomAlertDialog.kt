package org.zotero.android.uicomponents.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.modal.CustomAlertDialog.ButtonsStyle.COMPACT
import org.zotero.android.uicomponents.modal.CustomAlertDialog.ButtonsStyle.FULL
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CustomAlertDialog(
    title: String,
    description: String? = null,
    descriptionTextColor: Color = CustomTheme.colors.secondaryContent,
    primaryAction: CustomAlertDialog.ActionConfig,
    secondaryAction: CustomAlertDialog.ActionConfig? = null,
    onDismiss: () -> Unit,
    dismissOnClickOutside: Boolean = true,
    buttonsStyle: CustomAlertDialog.ButtonsStyle = COMPACT
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = dismissOnClickOutside,
            dismissOnBackPress = dismissOnClickOutside
        )
    ) {
        DialogContent(
            title = title,
            description = description,
            descriptionTextColor = descriptionTextColor,
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
            onDismiss = onDismiss,
            buttonsStyle = buttonsStyle
        )
    }
}

@Composable
@Preview
private fun DialogPreview() {
    CustomTheme {
        Column {
            DialogContent(
                title = "Delete item?",
                description = "Are you sure you want to delete this item?",
                primaryAction = CustomAlertDialog.ActionConfig(text = "Delete"),
                secondaryAction = CustomAlertDialog.ActionConfig(text = "Cancel"),
                descriptionTextColor = CustomTheme.colors.secondaryContent,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DialogContent(
                title = "Delete item?",
                description = "Are you sure you want to delete this item?",
                primaryAction = CustomAlertDialog.ActionConfig(text = "Delete"),
                secondaryAction = CustomAlertDialog.ActionConfig(text = "Cancel"),
                buttonsStyle = FULL,
                descriptionTextColor = CustomTheme.colors.secondaryContent,
            )
        }
    }
}

@Composable
private fun DialogContent(
    title: String,
    description: String? = null,
    descriptionTextColor: Color,
    primaryAction: CustomAlertDialog.ActionConfig,
    secondaryAction: CustomAlertDialog.ActionConfig? = null,
    onDismiss: () -> Unit = {},
    buttonsStyle: CustomAlertDialog.ButtonsStyle = COMPACT
) {
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(16.dp))
            .background(
                color = CustomTheme.colors.cardBackground,
                shape = RoundedCornerShape(16.dp),
            )
            .border(
                width = 1.dp,
                color = CustomTheme.colors.dialogBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .debounceClickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = {}
            )
    ) {
        TextSection(
            title = title,
            description = description,
            descriptionTextColor = descriptionTextColor,
        )
        CustomDivider(Modifier.padding(horizontal = 16.dp))
        when (buttonsStyle) {
            FULL -> {
                FullButtonsSection(
                    primaryAction = primaryAction,
                    secondaryAction = secondaryAction,
                    onDismiss = onDismiss,
                )
            }
            COMPACT -> {
                CompactButtonsSection(
                    primaryAction = primaryAction,
                    secondaryAction = secondaryAction,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun TextSection(
    title: String,
    description: String? = null,
    descriptionTextColor: Color,
) {
    Column(
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            text = title,
            style = CustomTheme.typography.h2
        )
        if (description != null) {
            Text(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                color = descriptionTextColor
            )
        }
    }
}

@Composable
private fun FullButtonsSection(
    primaryAction: CustomAlertDialog.ActionConfig,
    secondaryAction: CustomAlertDialog.ActionConfig? = null,
    onDismiss: () -> Unit = {},
) {
    DialogButton(
        actionConfig = primaryAction,
        textStyle = CustomTheme.typography.h3,
        modifier = Modifier.fillMaxWidth(),
        onDismiss = onDismiss,
        buttonsStyle = FULL,
    )

    if (secondaryAction != null) {
        CustomDivider(Modifier.padding(horizontal = 16.dp))
        DialogButton(
            actionConfig = secondaryAction,
            textStyle = CustomTheme.typography.default,
            modifier = Modifier.fillMaxWidth(),
            onDismiss = onDismiss,
            buttonsStyle = FULL,
        )
    }
}

@Composable
private fun ColumnScope.CompactButtonsSection(
    primaryAction: CustomAlertDialog.ActionConfig,
    secondaryAction: CustomAlertDialog.ActionConfig? = null,
    onDismiss: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .align(Alignment.End)
            .padding(8.dp)
    ) {
        if (secondaryAction != null) {
            DialogButton(
                actionConfig = secondaryAction,
                textStyle = CustomTheme.typography.h3,
                onDismiss = onDismiss,
                buttonsStyle = COMPACT,
            )
        }

        DialogButton(
            actionConfig = primaryAction,
            textStyle = CustomTheme.typography.h3,
            onDismiss = onDismiss,
            buttonsStyle = COMPACT,
        )
    }
}

@Composable
private fun DialogButton(
    actionConfig: CustomAlertDialog.ActionConfig,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    buttonsStyle: CustomAlertDialog.ButtonsStyle
) {
    TextButton(
        onClick = {
            if (actionConfig.dismissOnClick) onDismiss()
            actionConfig.onClick()
        },
        modifier = modifier,
        shape = RectangleShape,
    ) {
        val textModifier = when (buttonsStyle) {
            FULL -> Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            COMPACT -> Modifier
        }
        Text(
            text = actionConfig.text,
            modifier = textModifier,
            color = if (actionConfig.textColor == Color.Unspecified) {
                CustomTheme.colors.dynamicTheme.primaryColor
            } else {
                actionConfig.textColor
            },
            style = textStyle,
        )
    }
}

object CustomAlertDialog {
    data class ActionConfig(
        val text: String,
        val onClick: () -> Unit = {},
        val dismissOnClick: Boolean = true,
        val textColor: Color = Color.Unspecified,
    )

    /**
     * [FULL] buttons are laid out vertically and fill the dialog width. Should
     * be used when action texts are more than one word.
     * [COMPACT] buttons are smaller and should fit in one row. Should be used
     * when action texts are short and simple.
     */
    enum class ButtonsStyle {
        FULL, COMPACT
    }
}
