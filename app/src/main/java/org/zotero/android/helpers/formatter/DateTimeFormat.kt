package org.zotero.android.helpers.formatter

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

val dateAndTimeFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d, yyyy 'at' h:mm a z", Locale.getDefault())

val shortDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

val dateFormatItemDetails: SimpleDateFormat
    @SuppressLint("SimpleDateFormat")
    get() = SimpleDateFormat("dd.MM.yyyy, HH:mm:ss").apply {
//        timeZone = TimeZone.getTimeZone("UTC")
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

val deadlineTimeFormat: SimpleDateFormat
    get() = SimpleDateFormat("EEEE MMM d, h:mm a", Locale.getDefault())

val sqlFormat: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

val fullDateWithDashes: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())