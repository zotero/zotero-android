package org.zotero.android.architecture

sealed class LCE2 {
    object Loading : LCE2()
    object Content : LCE2()

    data class LoadError(val tryAgain: () -> Unit) : LCE2()
}
