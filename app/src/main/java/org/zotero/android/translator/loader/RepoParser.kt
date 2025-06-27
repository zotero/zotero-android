package org.zotero.android.translator.loader

import com.google.gson.Gson
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.zotero.android.sync.Translator
import java.io.InputStream

class RepoParser {
    private var inputStream: InputStream? = null
    private var gson: Gson? = null

    var translators: MutableList<Translator> = mutableListOf()
    var styles: MutableList<Pair<String, String>> = mutableListOf()
    var timestamp: Long = 0L
    private var currentTranslator: Translator? = null
    private var currentValue: String = ""
    private var currentStyleId: String? = null

    companion object {
        fun fromInputStream(inputStream: InputStream, gson: Gson): RepoParser {
            val sp = RepoParser()
            sp.inputStream = inputStream
            sp.gson = gson
            return sp
        }
    }

    private enum class Element(val str: String) {
        timestamp("currentTime"),
        translator("translator"),
        priority("priority"),
        label("label"),
        creator("creator"),
        target("target"),
        code("code"),
        style("style");

        val isTranslatorMetadata: Boolean
            get() {
                return when (this) {
                    timestamp, translator, style ->
                        false

                    priority, label, creator, target, code ->
                        true
                }
            }

        companion object {
            private val map = entries.associateBy(Element::str)
            fun from(s: String) = map[s]
        }
    }


    fun parseXml() {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val xpp = factory.newPullParser()
        xpp.setInput(this.inputStream, null)
        var eventType = xpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val element = Element.from(xpp.name)
                if (element == null) {
                    eventType = xpp.next()
                    continue
                }

                when (element) {
                    Element.translator -> {
                        this.currentTranslator = Translator.fromMetadata(
                            attributeCount = xpp.attributeCount,
                            getAttributeName = xpp::getAttributeName,
                            getAttributeValue = xpp::getAttributeValue,
                            code = "",
                            gson = gson!!
                        )
                    }

                    Element.style -> {
                        this.currentStyleId = xpp.getAttributeValue(null, "id")
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

                when (element) {
                    Element.timestamp -> {
                        this.timestamp = this.currentValue.toLongOrNull() ?: 0L
                    }

                    Element.translator -> {
                        val translator = this.currentTranslator
                        if (translator != null) {
                            this.translators.add(translator)
                            this.currentTranslator = null
                        }
                    }

                    Element.code -> {
                        this.currentTranslator =
                            this.currentTranslator?.withCode(this.currentValue)
                    }

                    Element.creator, Element.label, Element.priority, Element.target -> {
                        this.currentTranslator = this.currentTranslator?.withMetadata(
                            key = element.str,
                            value = this.currentValue,
                            gson = gson!!
                        )
                    }

                    Element.style -> {
                        val id = this.currentStyleId
                        if (id != null) {
                            this.styles.add(id to this.currentValue)
                        }
                    }
                }
                this.currentValue = ""


            } else if (eventType == XmlPullParser.TEXT) {
                this.currentValue += xpp.text
            }
            eventType = xpp.next()
        }
        return
    }
}