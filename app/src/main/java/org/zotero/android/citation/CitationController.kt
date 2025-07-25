package org.zotero.android.citation

import android.content.Context
import android.webkit.WebMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.ReadItemsWithKeysDbRequest
import org.zotero.android.database.requests.ReadStyleDbRequest
import org.zotero.android.database.requests.itemNotTypeIn
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.formatter.iso8601DateFormatV3
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.translator.data.WebPortResponse
import org.zotero.android.translator.helper.TranslatorHelper
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

class CitationController @Inject constructor(
    private val context: Context,
    dispatchers: Dispatchers,
    private val gson: Gson,
    private val citationWebViewHandler: CitationWebViewHandler,
    private val citationControllerPreviewUpdateEventStream: CitationControllerPreviewUpdateEventStream,
    private val citationControllerPreviewHeightUpdateEventStream: CitationControllerPreviewHeightUpdateEventStream,
    private val fileStore: FileStore,
    private val dbWrapperMain: DbWrapperMain,
    private val dbWrapperBundle: DbWrapperBundle,
) {

    companion object {
        val invalidItemTypes = setOf(ItemTypes.attachment, ItemTypes.note)
    }

    enum class Format {
        html,
        text,
        rtf,
    }

    private val ioCoroutineScope = CoroutineScope(dispatchers.io)

    private lateinit var itemIds: Set<String>
    private lateinit var libraryId: LibraryIdentifier
    private lateinit var styleId: String
    private lateinit var localeId: String
    private lateinit var itemsCSL: String
    private lateinit var styleXml: String
    private lateinit var localeXml: String
    private lateinit var styleLocaleId: String
    private lateinit var format: Format
    private var supportsBibliography: Boolean = false

    private var citationControllerInterface: CitationControllerInterface? = null

    fun init(
        citationControllerInterface: CitationControllerInterface,
        itemIds: Set<String>,
        libraryId: LibraryIdentifier,
        styleId: String,
        localeId: String
    ) {
        this.citationControllerInterface = citationControllerInterface
        this.itemIds = itemIds
        this.libraryId = libraryId
        this.styleId = styleId
        this.localeId = localeId

        val file = File(fileStore.citationDirectory(), "index.html")
        val filePath = "file://" + file.absolutePath
        loadWebPage(
            url = filePath,
            onWebViewLoadPage = ::onTranslatorIndexHtmlLoaded,
            processWebViewResponses = ::receiveMessage
        )
    }

    private fun receiveMessage(message: WebMessage) {
        ioCoroutineScope.launch {
            val data = message.data

            val mapType = object : TypeToken<WebPortResponse>() {}.type
            val decodedBody: WebPortResponse = gson.fromJson(data, mapType)
            val handlerName = decodedBody.handlerName
            val bodyElement = decodedBody.message
            when (handlerName) {
                "logHandler" -> {
                    Timber.i("CitationController JSLOG: ${bodyElement.asString}")
                }
                "cslHandler" -> {
                    val result = (bodyElement as JsonObject)["result"].asJsonArray
                    this@CitationController.itemsCSL =
                        TranslatorHelper.encodeAsJSONForJavascript(gson, result)
                    citation(
                        label = citationControllerInterface?.getLocator() ?: "",
                        locator = citationControllerInterface?.getLocatorValue() ?: "",
                        omitAuthor = citationControllerInterface?.omitAuthor() ?: false,
                        format = Format.html,
                        showInWebView = true
                    )
                }
                "citationHandler" -> {
                    val jsResult = (bodyElement as JsonObject)["result"].asString
                    val formatted = format(jsResult, this@CitationController.format)
                    citationControllerPreviewUpdateEventStream.emitAsync(formatted)
                }
                "heightHandler" -> {
                    val jsResult = bodyElement.asInt
                    citationControllerPreviewHeightUpdateEventStream.emitAsync(jsResult)
                }
            }
        }
    }


    private fun onTranslatorIndexHtmlLoaded() {
        ioCoroutineScope.launch {
            val styleData = loadStyleData(styleId)
            this@CitationController. styleLocaleId = styleData.defaultLocaleId ?: this@CitationController.localeId
            val (styleXml, localeXml) = loadEncodedXmls(
                styleFilename = styleData.filename,
                localeId = this@CitationController.styleLocaleId
            )
            this@CitationController.styleXml = styleXml
            this@CitationController.localeXml = localeXml

            this@CitationController.supportsBibliography = styleData.supportsBibliography
            val (schema, dateFormats) = loadBundleFiles()
            val itemJsons = loadItemJsons(
                keys = this@CitationController.itemIds,
                libraryId = this@CitationController.libraryId
            )
            getItemsCSL(itemJsons, schema, dateFormats)
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
            TranslatorHelper.encodeFileToBase64Binary(
                File(
                    fileStore.citationDirectory(),
                    "utilities/resource/schema/global/schema.json"
                )
            )
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

    fun loadItemJsons(keys: Set<String>, libraryId: LibraryIdentifier): String {
        try {
            val items = dbWrapperMain.realmDbStorage.perform(
                request = ReadItemsWithKeysDbRequest
                    (
                    keys = keys,
                    libraryId = libraryId
                )
            )
                .where()
                .itemNotTypeIn(CitationController.invalidItemTypes).findAll()

            if (items.isEmpty()) {
                throw Exception("Invalid Item Types")
            }

            val data = items.map({ data(it) })

            items.first()?.realm?.refresh()

            val result = TranslatorHelper.encodeStringToBase64Binary(gson.toJson(data))

            return result
        } catch (error: Exception) {
            Timber.e(error, "CitationController: can't read items")
            throw error
        }
    }

    fun data(item: RItem): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        for (field in item.fields) {
            data[field.key] = field.value
        }

        val creators: MutableList<Map<String, Any>> = mutableListOf()
        for (rCreator in item.creators.sort("orderId")) {
            val creator: MutableMap<String, Any> =
                mutableMapOf("creatorType" to rCreator.rawType)
            if (!rCreator.name.isEmpty()) {
                creator["name"] = rCreator.name
            } else {
                creator["firstName"] = rCreator.firstName
                creator["lastName"] = rCreator.lastName
            }
            creators.add(creator)
        }
        data["creators"] = creators

        val relations: MutableList<Map<String, Any>> = mutableListOf()
        for (rRelation in item.relations) {
            if (rRelation.urlString.isEmpty()) {
                continue
            }

            val urls = rRelation.urlString.split(";").filter { !it.isEmpty() }
            val relation = mutableMapOf<String, Any>()

            if (urls.size == 1) {
                val url = urls.first()
                relation[rRelation.type] = url
            } else {
                relation[rRelation.type] = urls
            }
            relations.add(relation)
        }
        data["relations"] = relations

        // Add remaining data
        data["key"] = item.key
        data["itemType"] = item.rawType
        data["version"] = item.version

        val key = item.parent?.key

        if (key != null) {
            data["parentItem"] = key
        }
        data["dateAdded"] = iso8601DateFormatV3.format(item.dateAdded)
        data["dateModified"] = iso8601DateFormatV3.format(item.dateModified)
        data["uri"] = "https://www.zotero.org/${item.key}"
        data["inPublications"] = item.inPublications
        data["collections"] = emptyArray<Any>()
        data["tags"] = emptyArray<Any>()

        return data
    }

    private suspend fun getItemsCSL(jsons: String, schema: String, dateFormats: String) {
        return suspendCancellableCoroutine { cont ->
            Timber.i("CitationWebViewHandler: call get items CSL js")
            citationWebViewHandler.evaluateJavascript("javascript:convertItemsToCSL('${jsons}', '${schema}', '${dateFormats}', 'msgid')") {
                cont.resume(Unit)
            }
        }
    }


    private suspend fun getCitation(
        itemsCSL: String,
        itemsData: String,
        styleXML: String,
        localeId: String,
        localeXML: String,
        format: String,
        showInWebView: Boolean
    ) {
        return suspendCancellableCoroutine { cont ->
            citationWebViewHandler.evaluateJavascript("javascript:getCit('${itemsCSL}', '${itemsData}', '${styleXML}', '${localeId}', '${localeXML}', '${format}', '${showInWebView}', 'msgid')") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun citation(itemIds: Set<String>? = null, label: String?, locator: String?, omitAuthor: Boolean, format: Format, showInWebView: Boolean) {
        val itemIds = itemIds ?: this.itemIds
        val itemsData =
            itemsData(itemIds, label = label, locator = locator, omitAuthor = omitAuthor)
        this.format = format
        getCitation(
            itemsCSL = this.itemsCSL,
            itemsData = itemsData,
            styleXML = this.styleXml,
            localeId = this.styleLocaleId,
            localeXML = this.localeXml,
            format = format.name,
            showInWebView = showInWebView
        )
    }

    fun itemsData(itemIds: Set<String>, label: String?, locator: String?, omitAuthor: Boolean): String {
        val itemsData = mutableListOf<Map<String, Any>>()
        for (key in itemIds) {
            val data = mutableMapOf("id" to "https://www.zotero.org/${key}", "suppress-author" to omitAuthor)
            if (label != null) {
                data["label"] = label
            }
            if (locator != null) {
                data["locator"] = locator
            }
            itemsData.add(data)
        }
        return TranslatorHelper.encodeAsJSONForJavascript(gson, itemsData)
    }

    private fun format(result: String, format: Format): String {
        when(format) {
            Format.rtf -> {
                var newResult = result
                if (!result.startsWith("{\\rtf")) {
                    newResult = "{\\rtf\n" + newResult
                }
                if (!result.endsWith("}")) {
                    newResult += "\n}"
                }
                return newResult
            }
            Format.html -> {
                var newResult = result
                if (!result.startsWith("<!DOCTYPE")) {
                    newResult = "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body>" + newResult
                }
                if (!result.endsWith("</html>")) {
                    newResult += "</body></html>"
                }
                return newResult
            }
            Format.text -> {
                return result
            }
        }
    }


}