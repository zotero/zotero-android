package org.zotero.android.uicomponents.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = CustomTheme.colors.dynamicTheme.primaryColor,
    pressedColor: Color = CustomTheme.colors.dynamicTheme.shadeOne,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Text(
        color = if (isPressed) pressedColor else contentColor,
        modifier = modifier
            .debounceClickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(8.dp),
        style = CustomTheme.typography.h3,
        text = text,
    )
}

@Composable
@Preview
private fun TextButtonPreview() {
    CustomTheme(isDarkTheme = true) {
        Column {
            TextButton(
                text = "JUST A BUTTON",
                onClick = {},
            )
        }
    }
}
