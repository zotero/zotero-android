package org.zotero.android.sync

import java.util.Random

object KeyGenerator {
    const val length = 8
    const val allowedChars = "23456789ABCDEFGHIJKLMNPQRSTUVWXYZ"

    private val random = Random()
    fun newKey(): String {
            var result = ""
            repeat(length) {
                result += allowedChars[random.nextInt(allowedChars.length)]
            }
            return result
        }
}