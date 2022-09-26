package org.zotero.android.sync
class Parsing {
    sealed class Error: Exception() {
        data class incompatibleValue(val str: String): Error()
        data class missingKey(val str: String): Error()
        object notArray: Error()
        object notDictionary: Error()
        object notUrl: Error()
    }
}
