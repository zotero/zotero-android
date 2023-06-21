package org.zotero.android.screens.libraries

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun LibrariesTopBar(
    onSettingsTapped: () -> Unit,
    ) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(id = Strings.libraries),
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.h3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {

        },
        actions = {
            Icon(
                modifier = Modifier.safeClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                    onClick = onSettingsTapped
                ),
                painter = painterResource(id = Drawables.baseline_settings_24),
                contentDescription = null,
                tint = CustomTheme.colors.dynamicTheme.primaryColor,
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )
}
