package org.zotero.android.screens.share

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ShareSection(
    content: @Composable ColumnScope.() -> Unit
) {
    val roundCornerShape = RoundedCornerShape(size = 10.dp)
    Column(
        modifier = Modifier
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = roundCornerShape
            )
            .clip(roundCornerShape),
    ) {
        content()
    }

}

@Composable
internal fun ShareItem(
    title: String,
    textColor: Color = CustomTheme.colors.primaryContent,
    onItemTapped: () -> Unit,
    addNewScreenNavigationIndicator: Boolean = false,
    addCheckmarkIndicator: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onItemTapped() },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 10.dp)
                .padding(end = 8.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textColor,
        )
        if (addNewScreenNavigationIndicator) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = Drawables.chevron_right_24px),
                contentDescription = null,
                tint = textColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (addCheckmarkIndicator) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = Drawables.check_24px),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroDefaultBlue
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
internal fun ShareSectionTitle(
    @StringRes titleId: Int
) {
    androidx.compose.material3.Text(
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        text = stringResource(id = titleId).uppercase(),
        fontSize = 14.sp,
        color = CustomTheme.colors.secondaryContent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun ParsedShareItem(
    title: String,
    @DrawableRes iconInt: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            modifier = Modifier.size(28.dp),
            painter = painterResource(id = iconInt),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .padding(end = 8.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.primaryContent,
        )
    }
}

@Composable
internal fun ShareErrorItem(
    title: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .padding(end = 8.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.error,
        )
    }
}

@Composable
internal fun ShareProgressItem(
    title: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .padding(end = 8.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = CustomPalette.CoolGray,
        )
        Spacer(modifier = Modifier.weight(1f))
        CircularProgressIndicator(
            color = CustomTheme.colors.zoteroDefaultBlue,
            modifier = Modifier
                .size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}


