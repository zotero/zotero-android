package org.zotero.android.ktx

inline fun <E> Collection<E>.index(element: E, sortedBy: (E, E) -> Boolean): Int {
    var (low, high) = (0 to this.size - 1)
    while (low <= high) {
        val mid = (low + high) / 2
        if (sortedBy(element, this.elementAt(mid))) {
            high = mid - 1
        } else if (sortedBy(this.elementAt(mid), element)) {
            low = mid + 1
        } else {
            return mid
        }
    }
    return low
}