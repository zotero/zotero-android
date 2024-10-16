package org.zotero.android.sync

import org.zotero.android.androidx.text.basicUnescape
import org.zotero.android.androidx.text.strippedHtmlTags

class NotePreviewGenerator {

    companion object {
        fun preview(note: String): String? {
            if (note.isEmpty()) {
                return null
            }
            var stripped = note.strippedHtmlTags.basicUnescape
            stripped = stripped.replace("\t", "")
            stripped = stripped.split("\\r\\n|\\n|\\r").firstOrNull() ?: stripped
            stripped = stripped.trim()

            val maxCharacters = 200
            if (stripped.length > maxCharacters) {
                stripped = stripped.take(maxCharacters)
            }
            if (stripped.isNotEmpty()) {
                //Has a valid surrogate pair at the very end of the stripped text
                if (stripped.hasSurrogatePairAt(stripped.length - 2)) {
                    return stripped
                }

                val lastChar = stripped[stripped.length - 1]
                //If last char is the surrogate without a pair
                if (lastChar.isLowSurrogate() || lastChar.isHighSurrogate()) {
                    return stripped.substring(0, stripped.length - 1)
                }
            }
            return stripped
        }
    }


}
