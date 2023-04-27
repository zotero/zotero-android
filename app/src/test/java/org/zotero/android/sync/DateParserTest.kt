package org.zotero.android.sync

import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class DateParserTest {
    private val parser = DateParser()

    @Test
    fun `returns null for empty string`() {
        runBlocking {
            parser.parse("") shouldBeEqualTo  null
        }
    }

    @Test
    fun `returns null for blank string`() {
        runBlocking {
            parser.parse(" ") shouldBeEqualTo  null
        }
    }

    @Test
    fun `should parse string with just number le 12 as month`() {
        runBlocking {
            val date = parser.parse("1")
            date?.day shouldBeEqualTo 0
            date?.month shouldBeEqualTo 1
            date?.year shouldBeEqualTo 0
            date?.order shouldBeEqualTo "m"
        }
    }

    @Test
    fun `should parse string with just number g 12 and le 31 as day`() {
        runBlocking {
            val date = parser.parse("30")
            date?.day shouldBeEqualTo 30
            date?.month shouldBeEqualTo 0
            date?.year shouldBeEqualTo 0
            date?.order shouldBeEqualTo "d"
        }
    }

    @Test
    fun `should parse string with just number g 100 as year`() {
        runBlocking {
            val date = parser.parse("2020")
            date?.day shouldBeEqualTo 0
            date?.month shouldBeEqualTo 0
            date?.year shouldBeEqualTo 2020
            date?.order shouldBeEqualTo "y"
        }
    }
    @Test
    fun `should parse three- and four-digit dates with leading zeros`() {
        runBlocking {
            parser.parse("001")?.year shouldBeEqualTo 1
            parser.parse("0001")?.year shouldBeEqualTo 1
            parser.parse("012")?.year shouldBeEqualTo 12
            parser.parse("0012")?.year shouldBeEqualTo 12
            parser.parse("0123")?.year shouldBeEqualTo 123
        }
    }
    @Test
    fun `should parse two-digit year greater than current year as previous century`() {
        runBlocking {
            parser.parse("1/1/99")?.year shouldBeEqualTo 1999
        }
    }
    @Test
    fun `should parse two-digit year less than or equal to current year as current century`() {
        runBlocking {
            parser.parse("1/1/01")?.year shouldBeEqualTo 2001
            parser.parse("1/1/11")?.year shouldBeEqualTo 2011
        }
    }
    @Test
    fun `should parse one-digit month and four-digit year`() {
        runBlocking {
            val date = parser.parse("8/2020")
            date?.day shouldBeEqualTo 0
            date?.month shouldBeEqualTo 8
            date?.year shouldBeEqualTo 2020
            date?.order shouldBeEqualTo "my"
        }
    }

    @Test
    fun `should parse two-digit month with leading zero and four-digit year`() {
        runBlocking {
            val date = parser.parse("08/2020")
            date?.day shouldBeEqualTo 0
            date?.month shouldBeEqualTo 8
            date?.year shouldBeEqualTo 2020
            date?.order shouldBeEqualTo "my"
        }
    }

    @Test
    fun `should parse month written as word`() {
        runBlocking {
            val date = parser.parse("January 2020")
            date?.day shouldBeEqualTo 0
            date?.month shouldBeEqualTo 1
            date?.year shouldBeEqualTo 2020
            date?.order shouldBeEqualTo "my"
        }
    }

    @Test
    fun `should parse day with suffix`() {
        runBlocking {
            val date = parser.parse("20th January 2020")
            date?.day shouldBeEqualTo 20
            date?.month shouldBeEqualTo 1
            date?.year shouldBeEqualTo 2020
            date?.order shouldBeEqualTo "dmy"
        }
    }
}