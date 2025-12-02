package org.zotero.android.pdf.reader.sidebar

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun Modifier.sectionHorizontalPadding(): Modifier {
    return this.padding(horizontal = 16.dp)
}

internal fun Modifier.sectionVerticalPadding(): Modifier {
    return this.padding(vertical = 8.dp)
}