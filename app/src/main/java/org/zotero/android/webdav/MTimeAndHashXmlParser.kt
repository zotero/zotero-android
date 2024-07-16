package org.zotero.android.webdav

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

object MTimeAndHashXmlParser {

    private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun readMtimeAndHash(input: String): Pair<Long, String> {
        val inputStream: InputStream =
            input.byteInputStream(StandardCharsets.UTF_8)
        inputStream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readData(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readData(parser: XmlPullParser): Pair<Long, String> {
        var mTime: Long = -1L
        var hash = ""
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "mtime" -> mTime = readMtime(parser).toLong()
                "hash" -> hash = readHash(parser)
                else -> skip(parser)
            }
        }
        return mTime to hash
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readMtime(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "mtime")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "mtime")
        return title
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readHash(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "hash")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "hash")
        return title
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}