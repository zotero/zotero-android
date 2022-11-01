package org.zotero.android.sync

import java.util.regex.Pattern

class UrlDetector {
    private val urlPattern: Pattern = Pattern.compile(
        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
    )

    fun isUrl(string: String): Boolean {
        val matcher = urlPattern.matcher(string)
        if (matcher.find()) {
            return matcher.end() >= string.length
        }
        return false
    }
}