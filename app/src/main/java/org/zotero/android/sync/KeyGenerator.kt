package org.zotero.android.sync

object KeyGenerator {
    val length = 8
    val allowedChars = "23456789ABCDEFGHIJKLMNPQRSTUVWXYZ"
        val newKey: String get() {
            var result = ""
            repeat(length) {
                result += allowedChars.random()
            }
            return result
    }
}