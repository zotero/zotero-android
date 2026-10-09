package org.zotero.android.speech

data class LocalUtterance(
    val id: String,
    val text: String,
    val pageOffset: Int,
    val isLast: Boolean,
) {
    companion object {
        private val sentenceEndings = listOf(". ", "! ", "? ", "; ")

        fun split(pageText: String, startIndex: Int, generation: Int, maxLength: Int): List<LocalUtterance> {
            val utterances = mutableListOf<LocalUtterance>()
            var position = startIndex
            while (position < pageText.length) {
                val end = chunkEnd(pageText, position, maxLength)
                utterances.add(
                    LocalUtterance(
                        id = "$generation-${utterances.size}",
                        text = pageText.substring(position, end),
                        pageOffset = position,
                        isLast = end >= pageText.length,
                    )
                )
                position = end
            }
            return utterances
        }

        fun generation(utteranceId: String): Int? {
            return utteranceId.substringBefore('-').toIntOrNull()
        }

        private fun chunkEnd(text: String, start: Int, maxLength: Int): Int {
            val limit = start + maxLength
            if (limit >= text.length) {
                return text.length
            }
            val window = text.substring(start, limit)
            val newline = window.lastIndexOf('\n')
            if (newline > 0) {
                return start + newline + 1
            }
            val sentenceEnd = sentenceEndings.maxOf { window.lastIndexOf(it) }
            if (sentenceEnd > 0) {
                return start + sentenceEnd + 2
            }
            val space = window.lastIndexOf(' ')
            if (space > 0) {
                return start + space + 1
            }
            return if (text[limit - 1].isHighSurrogate()) limit - 1 else limit
        }
    }
}