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
            stripped = stripped.replace("\t",  "")
            stripped = stripped.split("\\r\\n|\\n|\\r").firstOrNull() ?: stripped
            stripped = stripped.trim()

            val maxCharacters = 200
            if (stripped.length > maxCharacters) {
                stripped = stripped.take(maxCharacters)
            }
            return stripped
        }
    }


}
