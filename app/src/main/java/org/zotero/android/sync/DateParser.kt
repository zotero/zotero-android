package org.zotero.android.sync

import org.joda.time.DateTime
import org.joda.time.Months
import org.joda.time.YearMonth
import org.zotero.android.database.requests.ComponentDate
import timber.log.Timber
import java.util.Collections
import java.util.Locale
import java.util.regex.MatchResult
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateParser @Inject constructor() {

    private val enLocale: Locale = Locale("en_US")
    private var lastLocaleId: String? = null

    private val partsPattern =
        "^(.*?)\\b([0-9]{1,4})(?:([\\-\\/\\.\\u5e74])([0-9]{1,2}))?(?:([\\-\\/\\.\\u6708])([0-9]{1,4}))?((?:\\b|[^0-9]).*?)$"

    private val yearPattern =
        "^(.*?)\\b((?:circa |around |about |c\\.? ?)?[0-9]{1,4}(?: ?B\\.? ?C\\.?(?: ?E\\.?)?| ?C\\.? ?E\\.?| ?A\\.? ?D\\.?)|[0-9]{3,4})\\b(.*?)$"

    private val monthPattern =
        "^(.*)\\b(months)[^ ]*(?: (.*)$|$)"

    private val dayPattern =
        "\\b([0-9]{1,2})(?:suffixes)?\\b(.*)"

    private var months = emptyList<String>()
    private var monthExpression: Pattern? = null
    private var dayExpression: Pattern? = null

    private val partsExpression: Pattern?
        get() {
            return try {
                Pattern.compile(partsPattern)
            } catch (e: Exception) {
                Timber.e(e, "DateParser: can't create parts expression")
                null
            }
        }
    private val yearExpression: Pattern?
        get() {
            return try {
                Pattern.compile(yearPattern, Pattern.CASE_INSENSITIVE)
            } catch (e: Exception) {
                Timber.e(e, "DateParser: can't create year expression")
                null
            }
        }

    fun parse(string: String): ComponentDate? {
        val trimmed = string.trim().trim { it == '\n' }
        if (trimmed.isEmpty()) {
            return null
        }

        val parseData = ParseData()
        parseParts(trimmed, parseData)

        if (parseData.year == 0) {
            parseYear(parseData)
        }
        if (parseData.month == 0) {
            parseMonth(parseData)
        }
        if (parseData.day == 0) {
            parseDay(parseData)
        }

        if (((parseData.day > 0 && parseData.day <= 31)
                    || (parseData.month > 0 && parseData.month <= 12)
                    || parseData.year > 0)
            && !parseData.order.isEmpty()
        ) {
            return ComponentDate(
                day = parseData.day,
                month = parseData.month,
                year = parseData.year,
                order = parseData.order
            )
        }
        return null
    }

    private fun parseParts(string: String, parseData: ParseData) {
        val matcher = this.partsExpression!!.matcher(string)
        if (!matcher.find()) {
            return
        }
        val toMatchResult = matcher.toMatchResult()

        val preDatePart: String? = toMatchResult.substringOrNull(1)
        val datePart1 = toMatchResult.substringOrNull(2)
        val separator1 = toMatchResult.substringOrNull(3)
        val datePart2 = toMatchResult.substringOrNull(4)
        val separator2 = toMatchResult.substringOrNull(5)
        val datePart3 = toMatchResult.substringOrNull(6)
        val postDatePart = toMatchResult.substringOrNull(7)

        val shouldCheck =
            ((separator1.isJsNegative || separator2.isJsNegative)
                    || (separator1 == separator2)
                    || (separator1 == "\\u{5e74}"
                    && separator2 == "\\u{6708}"))
                    && ((datePart1?.isEmpty() == false
                    && datePart2?.isEmpty() == false
                    && datePart3?.isEmpty() == false)
                    || (preDatePart.isJsNegative && postDatePart.isJsNegative))

        if (!shouldCheck) {
            parseData.parts.add(Part(value = string, position = Part.Position.ending))
            return
        }

        // Inspect individual date parts

        if (datePart1?.length == 3 || datePart1?.length == 4 || separator1 == "\\u{5e74}") {
            parseData.day = datePart3.asInt
            parseData.month = datePart2.asInt
            parseData.year = year(datePart1)

            parseData.order =
                (if (parseData.year > 0) {
                    "y"
                } else {
                    ""
                }) + (if (parseData.month > 0) {
                    "m"
                } else {
                    ""
                }) + (if (parseData.day > 0) {
                    "d"
                } else {
                    ""
                })
        } else if (
            datePart1?.isEmpty() == false
            && datePart2.isJsNegative
            && datePart3?.isEmpty() == false
        ) {
            parseData.month = datePart1.asInt
            parseData.year = year(datePart3)
            parseData.order =
                (if (parseData.month > 0) "m" else "") + (if (parseData.year > 0) "y" else "")
        } else if (
            datePart1?.isEmpty() == false
            && datePart2.isJsNegative
            && datePart3.isJsNegative
        ) {
            val value = datePart1.asInt
            if (value <= 12) {
                parseData.month = value
                parseData.order = "m"
            } else if (value <= 31) {
                parseData.day = value
                parseData.order = "d"
            } else {
                parseData.year = value
                parseData.order = "y"
            }
        } else {
            val localeParts = Locale.getDefault().language.split("_")
            val country = if (localeParts.size == 2) localeParts[1] else "US"
            when (country) {
                "US",
                "FM",
                "PW",
                "PH" -> {
                    parseData.day = datePart2.asInt
                    parseData.month = datePart1.asInt
                    parseData.order =
                        (if (parseData.month > 0) {
                            "m"
                        } else {
                            ""
                        }) + (if (parseData.day > 0) {
                            "d"
                        } else {
                            ""
                        })
                }

                else -> {
                    parseData.day = datePart1.asInt
                    parseData.month = datePart2.asInt
                    parseData.order =
                        (if (parseData.day > 0) {
                            "d"
                        } else {
                            ""
                        }) + (if (parseData.month > 0) {
                            "m"
                        } else {
                            ""
                        })
                }
            }
            parseData.year = year(datePart3)
            if (parseData.year > 0) {
                parseData.order += "y"
            }
        }

        if (parseData.month > 12) {
            if (parseData.day == 0) {
                parseData.day = parseData.month
                parseData.month = 0
                parseData.order = parseData.order.replace("m", "d")
            } else if (parseData.day <= 12) {
                val tmpDay = parseData.day
                parseData.day = parseData.month
                parseData.month = tmpDay
                val dIdx = parseData.order.indexOf("d")
                val mIdx = parseData.order.indexOf("m")
                if (dIdx != -1 && mIdx != -1) {
                    val characters = parseData.order.toMutableList()
                    Collections.swap(characters, dIdx, mIdx)
                    parseData.order = characters.joinToString("")
                }
            }
        }

        if (parseData.day <= 31 && parseData.month <= 12) {
            if (preDatePart != null) {
                parseData.parts.add(Part(value = preDatePart, position = Part.Position.beginning))
            }
            if (postDatePart != null) {
                parseData.parts.add(Part(value = postDatePart, position = Part.Position.ending))
            }
        } else {
            // Parsed values were invalid, reset and try parsing individual values below.
            Timber.i("DateParser: partsExpression failed sanity check " +
                    "('$string' -> ${parseData.day} " +
                    "| ${parseData.month} " +
                    "| ${parseData.year} " +
                    "| '${parseData.order}')")
            parseData.day = 0
            parseData.month = 0
            parseData.year = 0
            parseData.order = ""
            parseData.parts.add(Part(value = string, position = Part.Position.ending))
        }
    }

    private fun createMonthsExpression(months: List<String>): Pattern? {
        Months.EIGHT
        val pattern = this.monthPattern
            .replace(
                "months", months.joinToString(separator = "|")
            )
        return try {
            Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            Timber.e(e, "DateParser: can't create month expression")
            null
        }
    }

    private fun createMonths(locale: Locale): List<String> {
        val months = longMonthsSymbols(locale)
        val allMonths = (months + shortMonthsSymbols(locale)).toMutableList()

        if (months != longMonthsSymbols(enLocale)) {
            allMonths.addAll(longMonthsSymbols(enLocale) + shortMonthsSymbols(enLocale))
        }
        return allMonths.map { it.lowercase() }
    }

    private val daySuffixes = "st,nd,rd,th"

    private fun createDayExpression(): Pattern? {
        val suffixes = daySuffixes.replace(",", "|")
        val pattern = this.dayPattern.replace("suffixes", suffixes)
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            Timber.e(e, "DateParser: can't create day expression")
            return null
        }
    }

    private fun shortMonthsSymbols(locale: Locale): List<String> {
        val list = mutableListOf<String>()
        for (i in 1..12) {
            val md = YearMonth(2000, i)
            list.add(md.monthOfYear().getAsShortText(locale))
        }
        return list
    }

    private fun longMonthsSymbols(locale: Locale): List<String> {
        val list = mutableListOf<String>()
        for (i in 1..12) {
            val md = YearMonth(2000, i)
            list.add(md.monthOfYear().getAsText(locale))
        }
        return list
    }

    private fun updateLocalizedExpressionsIfNeeded() {
        val locale = Locale.getDefault()
        if (this.lastLocaleId == locale.language) {
            return
        }
        this.months = createMonths(locale)
        this.monthExpression = createMonthsExpression(months = this.months)
        this.dayExpression = createDayExpression()
        this.lastLocaleId = locale.language
    }

    private fun year(string: String?): Int {
        if (string == null) {
            return 0
        }

        val year = string.toIntOrNull() ?: 0

        if (string.length > 2) {
            return year
        }

        val currentYear = DateTime.now().year
        val twoDigitYear = currentYear % 100
        val century = currentYear - twoDigitYear

        return if (year <= twoDigitYear) {
            century + year
        } else {
            century - 100 + year
        }
    }

    private fun update(order: String, part: Part, newPart: String): String {
        if (order.isEmpty()) {
            return newPart
        }

        when (part.position) {
            Part.Position.beginning -> {
                return newPart + order
            }

            Part.Position.ending -> {
                return order + newPart
            }

            is Part.Position.before -> {
                return order.replace(part.position.str, (newPart + part.position.str))
            }

            is Part.Position.after -> {
                return order.replace(part.position.str, (part.position.str + newPart))
            }
        }
    }

    private fun parseYear(parseData: ParseData) {
        for ((index, part) in parseData.parts.withIndex()) {
            if (part.value.isEmpty() || this.yearExpression == null) {
                continue
            }
            val matcher = this.yearExpression!!.matcher(part.value)
            if (!matcher.find()) {
                continue
            }
            val toMatchResult = matcher.toMatchResult()
            parseData.year = toMatchResult.substringOrNull(2).asInt
            if (parseData.year == 0) {
                continue
            }
            parseData.order = update(parseData.order, part, "y")
            parseData.parts.removeAt(index)
            parseData.parts.addAll(
                index = index, listOf(
                    Part(
                        value = (toMatchResult.substringOrNull(1) ?: "").trim(),
                        position = Part.Position.beginning
                    ),
                    Part(
                        value = (toMatchResult.substringOrNull(3) ?: "").trim(),
                        position = Part.Position.ending
                    )
                )
            )
            break

        }
    }

    private fun parseDay(parseData: ParseData) {
        updateLocalizedExpressionsIfNeeded()

        for ((index, part) in parseData.parts.withIndex()) {
            if (part.value.isEmpty() || this.dayExpression == null) {
                continue
            }
            val matcher = this.dayExpression!!.matcher(part.value)
            if (!matcher.find()) {
                continue
            }
            val toMatchResult = matcher.toMatchResult()
            parseData.day = toMatchResult.substringOrNull(1)
                ?.trim { !"0123456789".reversed().contains(it) }?.toIntOrNull() ?: 0

            if (parseData.day == 0 || parseData.day > 31) {
                continue
            }

            parseData.order = update(parseData.order, part, "d")
            val location = toMatchResult.start()
            var newPart = ""
            val postPart = toMatchResult.substringOrNull(2)
            if (postPart != null) {
                newPart = postPart
            }
            if (location > 0 && location < part.value.length) {
                newPart =
                    part.value.substring(0, location) + newPart
            }
            parseData.parts[index] = Part(value = newPart.trim(), position = Part.Position.ending)
            break
        }
    }

    private fun parseMonth(parseData: ParseData) {
        updateLocalizedExpressionsIfNeeded()
        for ((index, part) in parseData.parts.withIndex()) {
            if (part.value.isEmpty() || this.monthExpression == null) {
                continue
            }
            val matcher = this.monthExpression!!.matcher(part.value)
            if (!matcher.find()) {
                continue
            }
            val toMatchResult = matcher.toMatchResult()
            val monthString = toMatchResult.substringOrNull(2) ?: continue
            val monthIndex = this.months.indexOf(monthString.lowercase())
            if (monthIndex == -1) {
                break
            }

            parseData.month = (monthIndex % 12) + 1
            parseData.order = update(parseData.order, part, "m")
            parseData.parts.removeAt(index)
            parseData.parts.addAll(
                index = index, listOf(
                    Part(
                        value = (toMatchResult.substringOrNull(1) ?: "").trim(),
                        position = Part.Position.before("m")
                    ),
                    Part(
                        value = (toMatchResult.substringOrNull(3) ?: "").trim(),
                        position = Part.Position.after("m")
                    )
                )
            )
            break
        }
    }
}

private data class ParseData(
    var day: Int = 0,
    var month: Int = 0,
    var year: Int = 0,
    var order: String = "",
    var parts: MutableList<Part> = mutableListOf(),
)

private data class Part(
    val value: String,
    val position: Position,
) {
    sealed interface Position {
        object beginning : Position
        object ending : Position
        data class before(val str: String) : Position
        data class after(val str: String) : Position
    }
}

private val String?.isJsNegative: Boolean
    get() {
        return this.isNullOrEmpty()
    }

private val String?.asInt: Int
    get() {
        return this?.toIntOrNull() ?: 0
    }

fun MatchResult.substringOrNull(pos: Int): String? {
    return group(pos)
}