package org.zotero.android.sync

class NotePreviewGenerator {

    companion object {
        fun preview(note: String): String? {
            if (note.isEmpty()) {
                return null
            }
            var stripped = stripHtml(note)


            stripped = stripped.replace("\t",  "")
            stripped = stripped.split("\\r\\n|\\n|\\r").firstOrNull() ?: stripped
            stripped = stripped.trim()

            val maxCharacters = 200
            if (stripped.length > maxCharacters) {
                stripped = stripped.take(maxCharacters)
            }
            return stripped
        }

        private fun stripHtml(input: String): String {
            val regex = "<[^>]*>"
            return input.replace(regex.toRegex(), "")
        }
    }


}
