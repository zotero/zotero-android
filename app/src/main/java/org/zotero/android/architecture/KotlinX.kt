package org.zotero.android.architecture

/**
 * Used to force a when statement to be exhaustive.
 */
@Suppress("unused")
val Any?.exhaustive
    get() = Unit

fun <T> T?.require(): T = this ?: throw NullPointerException()

fun <T : CharSequence> T?.nonEmptyStringOrNull(): T? = if (this.isNullOrBlank()) {
    null
} else {
    this
}
