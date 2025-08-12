package org.zotero.android.helpers.formatter

import android.annotation.SuppressLint
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

val shortDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

fun dateFormatItemDetails(): DateFormat {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.getDefault())
}

val iso8601DateFormat: SimpleDateFormat
    @SuppressLint("SimpleDateFormat")
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

val iso8601DateFormatV2: SimpleDateFormat
    @SuppressLint("SimpleDateFormat")
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
val iso8601DateFormatV3: SimpleDateFormat
    @SuppressLint("SimpleDateFormat")
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

val fullDateWithDashesUtc: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

val deadlineTimeFormat: SimpleDateFormat
    get() = SimpleDateFormat("EEEE MMM d, h:mm a", Locale.getDefault())

val sqlFormat: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("en", "US", "POSIX")).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

val fullDateWithDashes: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())