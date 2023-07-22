package org.zotero.android.uicomponents.reorder

import timber.log.Timber

fun <T> List<T>.move(from: IntRange, to: Int): List<T> {
    if (from.first < 0 || from.last > size - 1) {
        Timber.e("From range [$from..$to] is out of bounds of the list")
        return this
    }
    if (to !in (0 until size)) {
        Timber.e("To index $to is out of bounds of the list")
        return this
    }
    val chunkToMove = subList(from.first, from.last + 1)
    return when {
        to > from.last -> ((subList(0, to + 1) - chunkToMove) + chunkToMove).let {
            if (to + 1 < size) it + subList(to + 1, size) else it
        }
        to < from.first -> subList(0, to) + chunkToMove + (subList(to, size) - chunkToMove)
        else -> this
    }
}

fun <T> List<T>.add(index: Int, element: T): List<T> {
    return toMutableList().apply { add(index, element) }.toList()
}

fun <T> List<T>.update(index: Int, update: T.() -> T): List<T> {
    return toMutableList().apply {
        val element = getOrNull(index)
        if (element != null) {
            set(index, element.update())
        }
    }.toList()
}

fun <T> List<T>.update(predicate: (T) -> Boolean, update: T.() -> T): List<T> {
    return toMutableList().apply {
        this.forEachIndexed { index, element ->
            if (predicate(element)) {
                set(index, element.update())
                return@forEachIndexed
            }
        }
    }
}
