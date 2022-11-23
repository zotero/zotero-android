package org.zotero.android.architecture.database.requests

import org.joda.time.DateTime
import java.lang.Integer.max
import java.util.Date


class ComponentDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val order: String
) {
    val date: Date?
        get() {
            if (this.year > 0) {
                return DateTime().withYear(year).withMonthOfYear(max(1, month))
                    .withDayOfMonth(max(1, day)).toDate()
            } else {
                return null
            }
        }
}
