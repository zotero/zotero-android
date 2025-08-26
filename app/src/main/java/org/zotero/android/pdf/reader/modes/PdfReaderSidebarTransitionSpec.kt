package org.zotero.android.pdf.reader.modes

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.ui.unit.IntOffset

internal fun AnimatedContentTransitionScope<Boolean>.pdfReaderSidebarTransitionSpec(): ContentTransform {
    val intOffsetSpec = tween<IntOffset>()
    return (slideInHorizontally(intOffsetSpec) { -it } with
            slideOutHorizontally(intOffsetSpec) { -it }).using(
        // Disable clipping since the faded slide-in/out should
        // be displayed out of bounds.
        SizeTransform(
            clip = false,
            sizeAnimationSpec = { _, _ -> tween() }
        ))
}
