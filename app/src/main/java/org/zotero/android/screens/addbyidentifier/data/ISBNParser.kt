package org.zotero.android.screens.addbyidentifier.data

object ISBNParser {

    private val isbnRegexPattern = "\\b(?:97[89]\\s*(?:\\d\\s*){9}\\d|(?:\\d\\s*){9}[\\dX])\\b"

    fun isbns(string: String): List<String> {
        val cleanedString = string.replace(Regex("[\\x2D\\xAD\\u2010-\\u2015\\u2043\\u2212]+"), "")
        val matches =
            Regex(isbnRegexPattern).findAll(cleanedString).map { it.value }.toMutableList()

        val isbns = mutableListOf<String>()

        for (match in matches) {
            val isbn = match.replace(Regex("\\s+"), "")

            if (if (isbn.length == 10) validate10(isbn) else validate13(isbn)) {
                isbns.add(isbn)
            }
        }

        return isbns
    }

    private fun validate10(isbn: String): Boolean {
        var sum = 0

        for (idx in 0..<10) {
            val startIndex = idx
            val endIndex = idx + 1
            val character = isbn.substring(startIndex, endIndex)

            val intValue = character.toIntOrNull()
            if (intValue != null) {
                sum += intValue * (10 - idx)
            } else if (idx == 9 && character == "X") {
                sum += 10
            } else {
                sum = 1
                break
            }
        }

        return sum % 11 == 0
    }

    private fun validate13(isbn: String): Boolean {
        var sum = 0

        for (idx in 0..<13) {
            val startIndex = idx
            val endIndex =  idx + 1
            val character = isbn.substring(startIndex, endIndex)

            val intValue = character.toIntOrNull()
            if (intValue == null) {
                sum = 1
                break
            }

            if (idx % 2 == 0) {
                sum += intValue
            } else {
                sum += intValue * 3
            }
        }

        return sum % 10 == 0
    }
}