package org.zotero.android.citation

import android.content.Context
import android.webkit.WebMessage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.requests.ReadStyleDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.translator.data.TranslatorActionEventStream
import org.zotero.android.translator.helper.TranslatorHelper
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class CitationController @Inject constructor(
    private val context: Context,
    dispatchers: Dispatchers,
    private val gson: Gson,
    private val citationWebViewHandler: CitationWebViewHandler,
    private val translatorActionEventStream: TranslatorActionEventStream,
    private val fileStore: FileStore,
    private val dbWrapperBundle: DbWrapperBundle,
) {
    private val ioCoroutineScope = CoroutineScope(dispatchers.io)

    private var itemSelectionMessageId: Long? = null

    private lateinit var itemIds: Set<String>
    private lateinit var libraryId: LibraryIdentifier
    private lateinit var styleId: String
    private lateinit var localeId: String

    fun init(
        itemIds: Set<String>,
        libraryId: LibraryIdentifier,
        styleId: String,
        localeId: String
    ) {
        this.itemIds = itemIds
        this.libraryId = libraryId
        this.styleId = styleId
        this.localeId = localeId

        val file = File(fileStore.citationDirectory(), "index.html")
        val filePath = "file://" + file.absolutePath
        loadWebPage(
            url = filePath,
            onWebViewLoadPage = ::onTranslatorIndexHtmlLoaded,
            processWebViewResponses = {}
        )
    }

    private fun onTranslatorIndexHtmlLoaded() {
        ioCoroutineScope.launch {
            val styleData = loadStyleData(styleId)
            val styleLocaleId = styleData.defaultLocaleId ?: localeId
            val (styleXml, localeXml) = loadEncodedXmls(styleFilename = styleData.filename, localeId = styleLocaleId)
            val supportsBibliography = styleData.supportsBibliography
            val (schema, dateFormats) = loadBundleFiles()
        }
    }

    fun loadStyleData(styleId: String): StyleData {
        try {
            val style = dbWrapperBundle.realmDbStorage.perform(
                request = ReadStyleDbRequest(
                    identifier = styleId
                )
            )
            val data = StyleData.fromRStyle(style = style)
            style.realm?.refresh()
            return data
        } catch (error: Exception) {
            Timber.e(error, "CitationController: can't load style")
            throw error
        }
    }

    fun loadEncodedXmls(styleFilename: String, localeId: String): Pair<String, String> {
        val localeUrl = File(fileStore.cslLocalesDirectory(), "locales-${localeId}.xml")
        if (!localeUrl.exists()) {
            Timber.e("CitationController: can't load locale xml")
            throw Exception("Style or locale is missing")
        }
        val styleData = fileStore.style(filenameWithoutExtension = styleFilename)

        val encodedLocaleData = TranslatorHelper.encodeFileToBase64Binary(localeUrl)
        val encodedStylesData = TranslatorHelper.encodeFileToBase64Binary(styleData)
        return encodedStylesData to encodedLocaleData
    }

    private fun loadBundleFiles(): Pair<String, String> {
        val encodedSchemaData =
            TranslatorHelper.encodeFileToBase64Binary(   File(
                fileStore.citationDirectory(),
                "utilities/resource/schema/global/schema.json"
            ))
        val encodedDateFormatData =
            TranslatorHelper.encodeFileToBase64Binary(
                File(
                    fileStore.citationDirectory(),
                    "utilities/resource/dateFormats.json"
                )
            )
        return encodedSchemaData to encodedDateFormatData
    }

    private fun loadWebPage(
        url: String,
        onWebViewLoadPage: () -> Unit,
        processWebViewResponses: (message: WebMessage) -> Unit
    ) {
        citationWebViewHandler.load(
            url = url,
            onWebViewLoadPage = onWebViewLoadPage,
            processWebViewResponses = processWebViewResponses
        )
    }
}