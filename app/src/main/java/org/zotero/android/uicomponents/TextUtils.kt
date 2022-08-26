package org.zotero.android.uicomponents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import org.zotero.android.architecture.content.AndroidText

@Composable
@ReadOnlyComposable
fun androidText(androidText: AndroidText): String {
    val context = LocalContext.current
    return androidText.toString(context)
}
