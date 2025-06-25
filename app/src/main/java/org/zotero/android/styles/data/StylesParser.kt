package org.zotero.android.styles.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.zotero.android.helpers.formatter.iso8601DateFormatV3
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.Date

class StylesParser {
    private var file: File? = null
    private var filename: String? = null
    private var currentValue: String = ""
    private var identifier: String? = null
    private var title: String? = null
    private var updated: Date? = null
    private var href: String? = null
    private var dependencyHref: String? = null
    private var supportsCitation: Boolean = false
    private var supportsBibliography: Boolean = false
    private var isNoteStyle: Boolean = false
    private var defaultLocale: String? = null

    companion object {
        fun fromFile(file: File): StylesParser {
            val sp = StylesParser()
            sp.file = file
            sp.filename = file.nameWithoutExtension
            sp.supportsCitation = false
            sp.supportsBibliography = false
            sp.isNoteStyle = false
            sp.currentValue = ""
            return sp
        }
    }

    private enum class Element(val str: String) {
        identifier("id"),
        title("title"),
        updated("updated"),
        link("link"),
        citation("citation"),
        bibliography("bibliography"),
        style("style");

        companion object {
            private val map = Element.values().associateBy(Element::str)
            fun from(s: String) = map[s]
        }
    }


    fun parseXml(): Style? {
        try {
            val inputStream = FileInputStream(this.file)
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(inputStream, null)
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val element = Element.from(xpp.name)
                    if (element == null) {
                        eventType = xpp.next()
                        continue
                    }

                    when (element) {
                        Element.link -> {
                            val rel = xpp.getAttributeValue(null, "rel")
                            if (rel == null) {
                                eventType = xpp.next()
                                continue
                            }
                            when (rel) {
                                "self" -> {
                                    if (this.href == null) {
                                        this.href = xpp.getAttributeValue(null, "href")
                                    }
                                }

                                "independent-parent" -> {
                                    this.dependencyHref = xpp.getAttributeValue(null, "href")
                                }
                            }
                        }

                        Element.style -> {
                            val locale = xpp.getAttributeValue(null, "default-locale")
                            if (locale != null) {
                                this.defaultLocale = locale
                            }
                            val classValue = xpp.getAttributeValue(null, "class")
                            if (classValue != null) {
                                this.isNoteStyle = classValue == "note"
                            }
                        }

                        else -> {
                            //no-op
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    val element = Element.from(xpp.name)
                    if (element == null) {
                        eventType = xpp.next()
                        this.currentValue = ""
                        continue
                    }

                    this.currentValue = this.currentValue.replace(Regex("[\\r\\n\\t]+"), "").trim()
                    when (element) {
                        Element.identifier -> {
                            this.identifier = this.currentValue
                        }

                        Element.title -> {
                            this.title = this.currentValue
                        }

                        Element.updated -> {
                            this.updated = iso8601DateFormatV3.parse(this.currentValue)
                        }

                        Element.citation -> {
                            this.supportsCitation = true
                        }

                        Element.bibliography -> {
                            this.supportsBibliography = true
                        }

                        else -> {
                            //no-op
                        }
                    }
                    this.currentValue = ""


                } else if (eventType == XmlPullParser.TEXT) {
                    this.currentValue += xpp.text
                }
                eventType = xpp.next()
            }

            //End of document
            if (!this.supportsCitation && this.dependencyHref == null) {
                Timber.e("Style \"${this.identifier ?: "unknown id"}\"; \"${this.filename ?: this.href?.substringAfterLast('/') ?: "unknown filename"}\" doesn't support citation")
                return null
            }
            val identifier = this.identifier
            val title = this.title
            val updated = this.updated
            val href = this.href
            if (identifier == null || title == null || updated == null || href == null) {
                return null
            }
            inputStream.close()
            val style = Style(
                identifier = identifier,
                dependencyId = this.dependencyHref,
                title = title,
                updated = updated,
                href = href,
                filename = (this.filename ?: href.substringAfterLast('/')),
                supportsBibliography = this.supportsBibliography,
                isNoteStyle = this.isNoteStyle,
                defaultLocale = this.defaultLocale
            )
            return style
        } catch (e: Exception) {
            Timber.e(e, "Error parsing Style: $filename")
        }
        return null
    }
}