package org.zotero.android.androidx.content

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import timber.log.Timber
import kotlin.math.roundToInt

fun Context.getDimension(@DimenRes res: Int): Float = resources.getDimension(res)

fun Context.getDimensionPx(@DimenRes res: Int): Int = resources.getDimensionPixelSize(res)

fun Context.getColorForAttribute(@AttrRes res: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(res, typedValue, true)
    return typedValue.data
}

fun Context.dpToPx(dp: Int): Int =
    (resources.displayMetrics.density * dp).roundToInt()

fun Context.spToPx(sp: Int): Int =
    (resources.displayMetrics.scaledDensity * sp).roundToInt()

fun Context.spToPx(sp: Float): Float = resources.displayMetrics.scaledDensity * sp

fun Context.pxToSp(px: Float): Float = px / resources.displayMetrics.scaledDensity

val Context.screenHeight
    get() = resources.displayMetrics.heightPixels

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        .show()
}

fun Context.longToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG)
        .show()
}

fun Context.toast(@StringRes res: Int) = toast(getString(res))

fun Context.longToast(@StringRes res: Int) = longToast(getString(res))

fun Context.isDarkTheme(): Boolean {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}

fun Context.hasAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, RECORD_AUDIO) == PERMISSION_GRANTED

fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED

fun Context.hasAllPermissions(permissions: List<String>): Boolean =
    permissions.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED
    }

fun Context.getFirstPrimaryClip(): ClipData.Item? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    return clipboard.primaryClip?.let { primaryClip ->
        if (primaryClip.itemCount > 0) {
            primaryClip.getItemAt(0)
        } else {
            null
        }
    }
}

fun Context.getFileSize(uri: Uri): Long? {
    try {
        val descriptor = contentResolver.openFileDescriptor(uri, "r")
        val size = descriptor?.statSize ?: return null
        descriptor.close()
        return size
    } catch (e: Resources.NotFoundException) {
        Timber.e(e, "Failed to get file size")
        return null
    }
}

@Composable
fun Context.getDrawableByItemType(typeIconString: String): Int {
    val drawableId = remember(typeIconString) {
        resources.getIdentifier(
            typeIconString,
            "drawable",
            packageName
        )
    }
    return drawableId
}

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }