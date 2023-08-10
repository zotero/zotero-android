package org.zotero.android.androidx.text

val String.strippedHtmlTags: String get() {
    if (this.isEmpty()) {
        return this
    }
    val regex = "<[^>]*>"
    return this.replace(regex.toRegex(), "")
}

val String.strippedRichTextTags: String get() {
    if (this.isEmpty()) {
        return this
    }
    val regex = "<\\/?[b|i|span|sub|sup][^>]*>"
    return this.replace(regex.toRegex(), "")
}

val String.basicUnescape: String get() {
    val characters = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'"
    )
    var str = this
    for ((escaped, unescaped) in characters) {
        str = str.replace(escaped, unescaped)
    }
    return str
}

