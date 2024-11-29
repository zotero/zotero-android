package org.zotero.android.uicomponents.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomTheme

/**
 * This is the Compose component for clear button is used to clear user entered content
 * [modifier] must contain the sizing and padding for the button's image.
 */
@Composable
fun TextFieldClearButton(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(id = Drawables.ic_delete_circular_small),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(CustomTheme.colors.secondaryContent),
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = ripple(bounded = false),
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentScale = contentScale,
    )
}

@Composable
@Preview
private fun PrimaryButtonPreviewDark() {
    CustomTheme(isDarkTheme = true) {
        Column {
            TextFieldClearButton(
                contentDescription = "Sample content description",
                modifier = Modifier
                    .size(48.dp)
                    .padding(12.dp),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun PrimaryButtonPreviewLight() {
    CustomTheme(isDarkTheme = false) {
        Column {
            TextFieldClearButton(
                contentDescription = "Sample content description",
                modifier = Modifier
                    .size(48.dp)
                    .padding(12.dp),
                onClick = {}
            )
        }
    }
}
