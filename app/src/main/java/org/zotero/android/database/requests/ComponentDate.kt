package org.zotero.android.database.requests

import org.joda.time.DateTime
import java.lang.Integer.max
import java.util.Date


class ComponentDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val order: String
) {
    val orderWithSpaces: String get() {
        return this.order.toCharArray().joinToString(separator = " ")
    }

    val date: Date?
        get() {
            if (this.year > 0) {
                val withMonthOfYear = DateTime().withYear(year).withMonthOfYear(max(1, month))
                val maxDayInThisMonth = withMonthOfYear.dayOfMonth().maximumValue
                if (day <= maxDayInThisMonth) {
                    return withMonthOfYear
                        .withDayOfMonth(max(1, day)).toDate()
                }
            }
            return null
        }
}
