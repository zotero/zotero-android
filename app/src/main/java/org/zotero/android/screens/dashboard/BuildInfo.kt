package org.zotero.android.screens.dashboard

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.BuildConfig
import org.zotero.android.architecture.ui.CustomLayoutSize

@Composable
fun ColumnScope.BuildInfo() {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = "Zotero (${BuildConfig.VERSION_NAME})",
        fontSize = layoutType.calculateBuildInfoTextSize(),
    )
    Spacer(modifier = Modifier.height(4.dp))
}