package org.zotero.android.architecture

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

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

fun <T> emptyImmutableSet(): ImmutableSet<T> = emptySet<T>().toImmutableSet()