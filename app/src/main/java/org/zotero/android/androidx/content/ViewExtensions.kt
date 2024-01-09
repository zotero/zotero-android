package org.zotero.android.androidx.content

import android.graphics.Color.WHITE
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.snackbar.Snackbar

fun View.longErrorSnackbar(
    @StringRes text: Int,
    @StringRes actionText: Int? = null,
    action: (() -> Unit)? = null,
) {
    val snackbar = Snackbar
        .make(this, text, Snackbar.LENGTH_LONG)

    if (action != null && actionText != null) {
        snackbar.setAction(actionText) { action.invoke() }
    }

    snackbar.show()
}

fun View.longErrorSnackbar(
    text: String,
    duration: Int? = null,
    @StringRes actionText: Int? = null,
    action: (() -> Unit)? = null,
) {
    val snackbar = Snackbar
        .make(this, text, Snackbar.LENGTH_LONG)
        .setTextColor(WHITE)

    if (action != null && actionText != null) {
        snackbar.setAction(actionText) { action.invoke() }
    }
    if (duration != null) {
        snackbar.duration = duration
    }

    snackbar.view.setBackgroundColor(Color(0xFFE0244D).toArgb());
    snackbar.show()
}
