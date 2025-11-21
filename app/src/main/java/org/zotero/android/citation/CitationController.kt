package org.zotero.android.citation

import android.content.Context
import android.webkit.WebMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.citation.data.InvalidItemTypesException
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CitationController @Inject constructor(
    private val context: Context,
    dispatchers: Dispatchers,
    private val gson: Gson,
    private val citationWebViewHandler: CitationWebViewHandler,
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

    private val responseHandlers = ConcurrentHashMap<String, CancellableContinuation<String>>()

    suspend fun startSession(
        itemIds: Set<String>,
        libraryId: LibraryIdentifier,
        styleId: String,
        localeId: String
    ): CitationSession {
        return suspendCancellableCoroutine { cont ->
            val file = File(fileStore.citationDirectory(), "index.html")
            val filePath = "file://" + file.absolutePath

            val onWebPageLoaded: () -> Unit = {
                ioCoroutineScope.launch {
                    val styleData = loadStyleData(styleId)
                    val styleLocaleId =
                        styleData.defaultLocaleId ?: localeId
                    val (styleXml, localeXml) = loadEncodedXmls(
                        styleFilename = styleData.filename,
                        localeId = styleLocaleId
                    )
                    val supportsBibliography = styleData.supportsBibliography
                    val (schema, dateFormats) = loadBundleFiles()
                    try {
                        val itemJsons = loadItemJsons(
                            keys = itemIds,
                            libraryId = libraryId
                        )
                        val itemCSL = getItemsCSL(itemJsons, schema, dateFormats)
                        val session = CitationSession(
                            itemIds = itemIds,
                            libraryId = libraryId,
                            styleXML = styleXml,
                            styleLocaleId = styleLocaleId,
                            localeXML = localeXml,
                            supportsBibliography = supportsBibliography,
                            itemsCSL = itemCSL
                        )
                        cont.resume(session)
                    }catch (e: Exception) {
                        cont.resumeWithException(e)
                    }

                }

            }

            loadWebPage(
                url = filePath,
                processWebViewResponses = ::receiveMessage,
                onWebViewLoadPage = onWebPageLoaded
            )
        }

    }

    private fun receiveMessage(message: WebMessage) {
        ioCoroutineScope.launch {
            val data = message.data

            val mapType = object : TypeToken<WebPortResponse>() {}.type
            val decodedBody: WebPortResponse = gson.fromJson(data, mapType)
            val handlerName = decodedBody.handlerName
            val bodyElement = decodedBody.message

            if (handlerName == "logHandler") {
                Timber.i("CitationController JSLOG: ${bodyElement.asString}")
                return@launch
            }

            if (handlerName == "heightHandler") {
                val jsResult = bodyElement.asInt
                citationControllerPreviewHeightUpdateEventStream.emitAsync(jsResult)
                return@launch
            }

            val body = (bodyElement as JsonObject)
            val id = body["id"].asString
            val jsResult = bodyElement["result"]

            var result: String
            when (handlerName) {
                "citationHandler", "bibliographyHandler" -> {
                    result = jsResult.asString
                }

                "cslHandler" -> {
                    result = TranslatorHelper.encodeAsJSONForJavascript(gson, jsResult.asJsonArray)
                }
                else -> {
                    return@launch
                }
            }
            val responseHandler:CancellableContinuation<String>? = responseHandlers[id]
            if (responseHandler != null) {
                responseHandler.resume(result)
            } else {
                Timber.e("CitationController: response handler for $handlerName with id $id doesn't exist anymore")
            }

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
                .itemNotTypeIn(invalidItemTypes).findAll()

            if (items.isEmpty()) {
                throw InvalidItemTypesException()
            }

            val data = items.map { data(it) }

            items.first()?.realm?.refresh()

            val result = TranslatorHelper.encodeStringToBase64Binary(gson.toJson(data))

            return result
        } catch (error: Exception) {
            if (error !is InvalidItemTypesException) {
                Timber.e(error, "CitationController: can't read items")
            }
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

    private suspend fun getItemsCSL(jsons: String, schema: String, dateFormats: String): String {
        val javascript =
            "javascript:convertItemsToCSL('${jsons}', '${schema}', '${dateFormats}', 'msgid')"
        Timber.i("CitationWebViewHandler: call get items CSL js")
        return executeJavascript(javascript)
    }


    private suspend fun getCitation(
        itemsCSL: String,
        itemsData: String,
        styleXML: String,
        localeId: String,
        localeXML: String,
        format: String,
        showInWebView: Boolean
    ): String {
        val javascript =
            "javascript:getCit('${itemsCSL}', '${itemsData}', '${styleXML}', '${localeId}', '${localeXML}', '${format}', '${showInWebView}', 'msgid')"
        return executeJavascript(javascriptStr = javascript)
    }

    private suspend fun executeJavascript(javascriptStr: String): String {
        return suspendCancellableCoroutine { cont ->
            val id = UUID.randomUUID().toString()
            responseHandlers.put(id, cont)
            citationWebViewHandler.evaluateJavascript(javascriptStr.replace("msgid", id)) {
//                cont.resume(Unit)
            }
        }
    }

    suspend fun citation(
        session: CitationSession,
        itemIds: Set<String>? = null,
        label: String?,
        locator: String?,
        omitAuthor: Boolean,
        format: Format,
        showInWebView: Boolean
    ): String {
        val itemIds = itemIds ?: session.itemIds
        val itemsData =
            itemsData(itemIds, label = label, locator = locator, omitAuthor = omitAuthor)
        val result = getCitation(
            itemsCSL = session.itemsCSL,
            itemsData = itemsData,
            styleXML = session.styleXML,
            localeId = session.styleLocaleId,
            localeXML = session.localeXML,
            format = format.name,
            showInWebView = showInWebView
        )
        return format(result, format)
    }

    fun itemsData(
        itemIds: Set<String>,
        label: String?,
        locator: String?,
        omitAuthor: Boolean
    ): String {
        val itemsData = mutableListOf<Map<String, Any>>()
        for (key in itemIds) {
            val data = mutableMapOf(
                "id" to "https://www.zotero.org/${key}",
                "suppress-author" to omitAuthor
            )
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
        when (format) {
            Format.rtf -> {
                var newResult = result
                if (!result.startsWith("{\\rtf")) {
                    newResult = "{\\rtf\n$newResult"
                }
                if (!result.endsWith("}")) {
                    newResult += "\n}"
                }
                return newResult
            }

            Format.html -> {
                var newResult = result
                if (!result.startsWith("<!DOCTYPE")) {
                    newResult =
                        "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body>$newResult"
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

    private suspend fun getBibliography(
        itemsCSL: String,
        styleXML: String,
        localeId: String,
        localeXML: String,
        format: String,
    ): String {
        val javascript =
            "javascript:getBib('${itemsCSL}', '${styleXML}', '${localeId}', '${localeXML}', '${format}', 'msgid')"
        return executeJavascript(javascript)
    }

    fun formatBibliography(citations: List<String>, format: Format): String {
        when (format) {
            Format.html -> {
                return "<ol>\n\t<li>" + citations.joinToString(separator = "</li>\n\t<li>") + "</li>\n</ol>"
            }

            Format.rtf -> {
                val prefix =
                    "{\\*\\listtable{\\list\\listtemplateid1\\listhybrid{\\listlevel\\levelnfc0\\levelnfcn0\\leveljc0\\leveljcn0\\levelfollow0\\levelstartat1" +
                            "\\levelspace360\\levelindent0{\\*\\levelmarker \\{decimal\\}.}{\\leveltext\\leveltemplateid1\\'02\\'00.;}{\\levelnumbers\\'01;}\\fi-360\\li720\\lin720 }" +
                            "{\\listname ;}\\listid1}}\n{\\*\\listoverridetable{\\listoverride\\listid1\\listoverridecount0\\ls1}}\n\\tx720\\li720\\fi-480\\ls1\\ilvl0\n"
                return prefix + citations.mapIndexed { index, t ->
                    "{\\listtext ${index + 1}.    }${t}\\\n"
                }.joinToString(separator = "")
            }


            Format.text -> {
                return citations.mapIndexed { index, t ->
                    "${index + 1}. $t"
                }.joinToString(separator = "\r\n")

            }
        }
    }

    suspend fun bibliography(session: CitationSession, format: Format): String {
        if (session.supportsBibliography) {
            val bibliography = getBibliography(
                itemsCSL = session.itemsCSL,
                styleXML = session.styleXML,
                localeId = session.styleLocaleId,
                localeXML = session.localeXML,
                format = format.name
            )
            return format(result = bibliography, format = format)
        }
        val numberedBibliography =
            numberedBibliography(session = session, itemIds = session.itemIds, format = format)
        return format(result = numberedBibliography, format = format)

    }

    suspend fun numberedBibliography(
        session: CitationSession,
        itemIds: Set<String>,
        format: Format
    ): String {
        val citations = itemIds.map {
            citation(
                session = session,
                itemIds = setOf(it),
                label = null,
                locator = null,
                omitAuthor = false,
                format = format,
                showInWebView = false
            )
        }
        return formatBibliography(citations, format)
    }


}