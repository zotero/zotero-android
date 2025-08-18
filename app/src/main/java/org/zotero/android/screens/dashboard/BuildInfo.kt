package org.zotero.android.screens.dashboard

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.zotero.android.BuildConfig
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun ColumnScope.BuildInfo() {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = "Zotero (${BuildConfig.VERSION_NAME})",
        style = CustomTheme.typography.newFootnote,
        color = Color(0xFF8E8E92),
    )
}