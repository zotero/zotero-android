package org.zotero.android.uicomponents.error

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.button.PrimaryButton
import org.zotero.android.uicomponents.button.PrimaryButtonSmall
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun FullScreenError(
    modifier: Modifier = Modifier,
    errorTitle: String = "",
    errorDescription: String = stringResource(Strings.error_list_load_body),
    errorButtonText: String = stringResource(Strings.error_list_load_refresh),
    errorAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ErrorIcon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center,
            text = errorTitle,
            style = CustomTheme.typography.h1,
            color = CustomTheme.colors.primaryContent,
        )
        Text(
            text = errorDescription,
            textAlign = TextAlign.Center,
            style = CustomTheme.typography.default,
            color = CustomTheme.colors.primaryContent,
        )
        if (errorButtonText.isNotBlank() && errorAction != null) {
            PrimaryButton(
                modifier = Modifier
                    .defaultMinSize(minWidth = 270.dp)
                    .padding(top = 28.dp),
                text = errorButtonText,
                onClick = { errorAction() },
            )
        }
    }
}

@Composable
fun SectionError(
    modifier: Modifier = Modifier,
    errorDescription: String = stringResource(Strings.error_list_load_body),
    errorButtonText: String = stringResource(Strings.error_list_load_refresh),
    errorAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ErrorIcon()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = errorDescription,
            textAlign = TextAlign.Center
        )
        if (errorButtonText.isNotBlank() && errorAction != null) {
            PrimaryButtonSmall(
                modifier = Modifier.padding(top = 20.dp),
                text = errorButtonText,
                onClick = { errorAction() },
                backgroundColor = CustomTheme.colors.zoteroDefaultBlue
            )
        }
    }
}

@Composable
fun ErrorIcon(
    background: Color = backgroundErrorColor(),
    iconTint: Color = CustomPalette.ErrorRed
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
    ) {
        Icon(
            painter = painterResource(id = Drawables.ic_alert_icon),
            contentDescription = stringResource(id = Strings.error),
            modifier = Modifier.align(Alignment.Center),
            tint = iconTint
        )
    }
}

@Composable
private fun backgroundErrorColor(): Color {
    return if (CustomTheme.colors.isLight) {
        CustomPalette.ErrorRedLight
    } else {
        CustomPalette.ErrorRedDark
    }
}
