package org.zotero.android.translator.loader

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.zotero.android.BuildConfig
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.requests.ReadStylesDbRequest
import org.zotero.android.database.requests.SyncRepoResponseDbRequest
import org.zotero.android.database.requests.SyncStylesDbRequest
import org.zotero.android.database.requests.SyncTranslatorsDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.Unzipper
import org.zotero.android.screens.share.data.TranslatorMetadata
import org.zotero.android.styles.data.Style
import org.zotero.android.styles.data.StylesParser
import org.zotero.android.sync.Translator
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslatorsAndStylesLoader @Inject constructor(
    dispatchers: Dispatchers,
    private val context: Context,
    private val gson: Gson,
    private val defaults: Defaults,
    private val itemsUnzipper: TranslatorItemsUnzipper,
    private val unzipper: Unzipper,
    private val fileStore: FileStore,
    private val bundleDbWrapper: DbWrapperBundle,
    private val nonZoteroApi: NonZoteroApi,
) {
    enum class UpdateType(val i: Int) {
        manual(1),
        initial(2),
        startup(3),
        notification(4),
        shareExtension(5);
    }
    sealed class Error : Exception() {
        object expired : Error()
        data class bundleLoading(val exception: Exception) : Error()
        object bundleMissing : Error()
        object incompatibleDeleted : Error()
        object cantParseXmlResponse : Error()
        object cantConvertTranslatorToData : Error()
        object translatorMissingId : Error()
        object translatorMissingLastUpdated : Error()

        val isBundleLoadingError: Boolean
            get() {
                return when (this) {
                    is bundleLoading -> {
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
    }

    private val uuidExpression: Pattern?
        get() {
            return try {
                Pattern.compile("setTranslator\\(['\"](?<uuid>[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12})['\"]\\)")
            } catch (e: Exception) {
                Timber.e(e, "can't create parts expression")
                null
            }
        }

    private var uiScope = CoroutineScope(dispatchers.main)

    fun translators(url: String?): String {
        val translators = loadTranslators(url)
        val translatorsJson = gson.toJson(translators)
        return translatorsJson
    }

    private fun loadTranslators(url: String?): List<Map<String, Any>> {
        try {
            val loadedUuids = mutableSetOf<String>()
            val allUuids = fileStore.translatorItemsDirectory().listFiles()?.map { it.name } ?: emptyList()
            val translators =
                loadTranslatorsWithDependencies(allUuids.toSet(), url, loadedUuids = loadedUuids)
            return translators
        } catch (e: Exception) {
            Timber.e(e, "can't load translators")
            throw e
        }
    }

    private fun loadTranslatorsWithDependencies(
        uuids: Set<String>,
        url: String?,
        loadedUuids: MutableSet<String>
    ): List<Map<String, Any>> {
        if (uuids.isEmpty()) {
            return emptyList()
        }

        val translators = mutableListOf<Map<String, Any>>()
        var dependencies = mutableSetOf<String>()

        for (uuid in uuids) {
            if (loadedUuids.contains(uuid)) {
                continue
            }
            val translator =
                loadRawTranslator(fileStore.translator(uuid), url)
                    ?: continue
            val id = translator["translatorID"] as? String ?: continue

            loadedUuids.add(id)
            translators.add(translator)
            val deps = findDependencies(translator).subtract(loadedUuids).subtract(loadedUuids)
            dependencies = dependencies.union(deps).toMutableSet()
        }

        translators.addAll(
            loadTranslatorsWithDependencies(
                dependencies,
                null,
                loadedUuids = loadedUuids
            )
        )

        return translators
    }

    private fun findDependencies(translator: Map<String, Any>): Set<String> {
        val code = translator["code"] as? String
        if (code == null) {
            Timber.e("raw translator missing code")
            return emptySet()
        }
        val uuidRegex = this.uuidExpression ?: return emptySet()
        val m = uuidRegex.matcher(code)
        val matches = mutableListOf<String>()
        while (m.find()) {
            val depStr = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                m.group("uuid")!!
            } else {
                val str = m.group()
                val firstQuotaIndex = str.indexOfFirst { it == '\'' || it == '"' }
                val lastQuotaIndex = str.indexOfLast { it == '\'' || it == '"' }
                if (firstQuotaIndex == lastQuotaIndex) {
                    str
                } else {
                    str.substring(firstQuotaIndex + 1, lastQuotaIndex)
                }
            }
            matches.add(depStr)
        }
        return matches.toSet()
    }

    private fun loadRawTranslator(
        file: File,
        url: String? = null
    ): Map<String, Any>? {
        val rawString: String
        try {
            rawString = FileHelper.readFileToString(file)
        } catch (e: Exception) {
            Timber.e(e, " can't create string from data")
            return null
        }
        val metadataEndIndex = metadataIndex(rawString) ?: return null
        val metadataData = rawString.substring(0, metadataEndIndex)

        val metadata: MutableMap<String, Any>
        try {
            val mapType = object : TypeToken<Map<String?, Any?>?>() {}.type
            val decodedBody = gson.fromJson<Map<String, Any>>(metadataData, mapType)

            metadata = decodedBody.toMutableMap()
        } catch (error: Exception) {
            Timber.e(error, "can't parse metadata")
            return null
        }
        val target = metadata["target"] as? String
        if (target == null) {
            Timber.e(
                "${(metadata["label"] as? String) ?: "unknown"} raw translator missing target"
            )
            return null
        }
        if (url != null && !target.isEmpty()) {
            try {
                val regularExpression: Pattern = Pattern.compile(target)
                if (!regularExpression.matcher(url).find()) {
                    return null
                }
                Timber.e(
                    "${(metadata["label"] as? String) ?: "unknown"} matches url"
                )
            } catch (error: Exception) {
                Timber.e(
                    error,
                    "can't create regular expression '$target'",

                    )
                return null
            }
        }

        metadata["code"] = rawString
        val value = metadata["type"]
        if (value != null) {
            metadata["translatorType"] = value
            metadata.remove("type")
        }
        val id = metadata["id"]
        if (id != null) {
            metadata["translatorID"] = id
            metadata.remove("id")
        }

        return metadata
    }

    private fun metadataIndex(string: String): Int? {
        var count = 0

        for ((index, character) in string.iterator().withIndex()) {
            if (character == '{') {
                count += 1
            } else if (character == '}') {
                count -= 1
            }

            if (count == 0) {
                return index + 1
            }
        }
        return null
    }

    private fun loadIndex(): List<TranslatorMetadata> {
        val inputStream = context.assets.open("translators/index.json")
        val array = gson.fromJson(InputStreamReader(inputStream), JsonArray::class.java)
        return array.map {
            it as JsonObject
            TranslatorMetadata.from(
                id = it["id"].asString,
                filename = it["fileName"].asString,
                rawLastUpdated = it["lastUpdated"].asString
            )
        }
    }

    private suspend fun _updateTranslatorsFromBundle(forceUpdate: Boolean) {
        val hash = loadLastTranslatorCommitHash()
        Timber.i("TranslatorsAndStylesLoader: should update translators from bundle, forceUpdate=$forceUpdate; oldHash=${defaults.getLastTranslatorCommitHash()}; newHash=$hash")
        if (!forceUpdate && defaults.getLastTranslatorCommitHash() == hash) {
            return
        }
        Timber.i("TranslatorsAndStylesLoader: update translators from bundle")

        val (deletedVersion, deletedIndices) = loadDeleted()
        syncTranslatorsWithBundledData(deleteIndices = deletedIndices, forceUpdate = forceUpdate)

        defaults.setLastTranslatorDeleted(deletedVersion)
        defaults.setLastTranslatorCommitHash(hash)
    }

    private fun loadLastTranslatorCommitHash(): String {
        return loadFromBundle(resource = "translators/commit_hash.txt", map = { it })
    }

    private fun loadLastStylesCommitHash(): String {
        return loadFromBundle(resource = "styles/commit_hash.txt", map = { it })
    }

    private fun loadDeleted(): Pair<Long, List<String>> {
        return loadFromBundle("translators/deleted.txt", map = {
            try {
                val data =
                    parse(deleted = it, lastDeletedVersion = defaults.getLastTranslatorDeleted())
                        ?: throw Error.incompatibleDeleted
                return data
            } catch (e: Exception) {
                Timber.e(e)
                throw Error.incompatibleDeleted
            }
        })
    }

    private fun parse(deleted: String, lastDeletedVersion: Long): Pair<Long, List<String>>? {
        val deletedLines = deleted.split("\n")
        if (deletedLines.isEmpty()) {
            return null
        }
        val version = parseDeleted(line = deletedLines[0])?.toLongOrNull() ?: return null

        if (version <= lastDeletedVersion) {
            return lastDeletedVersion to emptyList()
        }
        val indices = (1..<deletedLines.size).mapNotNull { parseDeleted(line = deletedLines[it]) }

        return version to indices
    }

    private fun parseDeleted(line: String): String? {
        val index = line.indexOfFirst { it.isWhitespace() || it == '\n' }
        if (index == -1) {
            return null
        }
        return line.substring(0, index)
    }


    private suspend fun syncTranslatorsWithBundledData(deleteIndices: List<String>, forceUpdate: Boolean) {
        Timber.i("TranslatorsAndStylesLoader: load index")
        val metadata = loadIndex()
        Timber.i("TranslatorsAndStylesLoader: sync translators to database")
        val request = SyncTranslatorsDbRequest(
            updateMetadata = metadata,
            deleteIndices = deleteIndices,
            forceUpdate = forceUpdate,
            fileStore = this.fileStore
        )
        var updated: List<Pair<String, String>> = emptyList()
        uiScope.launch {
            updated = bundleDbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
        }.join()

        Timber.i("TranslatorsAndStylesLoader: updated $updated.size translators")
        deleteIndices.forEach { id ->
            fileStore.translator(id).delete()
        }
        // Unzip updated translators
        Timber.i("TranslatorsAndStylesController: unzip translators")
        itemsUnzipper.unzip(translators = updated)
        Timber.i("TranslatorsAndStylesController: unzipped translators")

    }

    private inline fun <reified Result> loadFromBundle(
        resource: String,
        map: (String) -> Result
    ): Result {
        try {
            val inputStream = context.assets.open(resource)
            val rawValue = FileHelper.toString(inputStream)
            return map(rawValue.trim().trim { it == '\n' })
        } catch (e: Exception) {
            Timber.e(e)
            throw Error.bundleMissing
        }
    }

    private suspend fun updateFromBundle() {
        try {
            _updateTranslatorsFromBundle(forceUpdate = false)
            _updateStylesFromBundle()
            val timestamp = loadLastTimestamp()
            if (timestamp > defaults.getLastTimestamp()) {
                defaults.setLastTimestamp(timestamp)
                return
            } else {
                return
            }
            
        } catch (error: Exception) {
            Timber.e(error, "TranslatorsAndStylesLoader: can't update from bundle")
            throw Error.bundleLoading(error)
        }
    }

    private suspend fun _updateStylesFromBundle() {
        val hash = loadLastStylesCommitHash()

        if (defaults.getLastStylesCommitHash() == hash) {
            return
        }

        Timber.i("TranslatorsAndStylesLoader: update styles from bundle")

        syncStylesWithBundledData()

        defaults.setLastStylesCommitHash(hash)
    }

    private suspend fun syncStylesWithBundledData() {
        fileStore.stylesBundleExportDirectory().deleteRecursively()
        unzipper.unzipStream(
            zipInputStream = context.assets.open("styles/styles.zip"),
            location = fileStore.stylesBundleExportDirectory().absolutePath
        )
        val files = fileStore.stylesBundleExportDirectory().listFiles() ?: emptyArray()
        val styles = files.mapNotNull { file ->
            if (file.extension != "csl") {
                return@mapNotNull null
            }
            parseStyle(file)
        }
        val request = SyncStylesDbRequest(styles = styles)
        var updated = listOf<String>()
        uiScope.launch {
            updated =
                bundleDbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
        }.join()

        Timber.i("TranslatorsAndStylesController: updated ${updated.size} styles")
        val filteredFiles = files.filter { updated.contains(it.nameWithoutExtension) }
        for (file in filteredFiles) {
            val toFile = fileStore.style(filenameWithoutExtension = file.nameWithoutExtension)
            if (toFile.exists()) {
                toFile.delete()
            }
            FileHelper.copyFile(file, toFile)
        }
    }

    private fun splitStyles(styles: List<Pair<String, String>>): Pair<List<Style>, List<Pair<String, ByteArray>>> {
        val stylesMetadata = mutableListOf<Style>()
        val stylesData = mutableListOf<Pair<String, ByteArray>>()

        for ((_, xml) in styles) {

            val parser = StylesParser.fromString(xml)

            val style =  parser.parseXml() ?:continue

            stylesMetadata.add(style)
            stylesData.add(style.filename to FileHelper.toByteArray(xml))
        }

        return stylesMetadata to stylesData
    }


    private fun parseStyle(file: File): Style {
        val parser = StylesParser.fromFile(file)

        val style =  parser.parseXml()
        if (style != null) {
            return style
        }

        throw Error.cantParseXmlResponse
    }

    private fun loadLastTimestamp(): Long {
        return loadFromBundle(resource = "timestamp.txt", map = {
            try {
                return it.toLong()
            } catch (e: Exception) {
                Timber.e(e)
                throw Error.bundleMissing
            }
        })
    }

    suspend fun updateTranslatorItemsIfNeeded() {
        _update()
    }

    private suspend fun _update() {
        val type: UpdateType =
            if (defaults.getLastTimestamp() == 0L) {
                UpdateType.initial
            } else {
                UpdateType.startup
            }


        Timber.i("TranslatorsAndStylesLoader: update translators and styles")
        try {
            checkFolderIntegrity(type = type)
            updateFromBundle()
            _updateFromRepo(type = type)
            defaults.setLastTimestamp(System.currentTimeMillis() / 1000)
        } catch (error: Exception) {
            process(error = error, updateType = type)
        }
    }

    suspend fun _updateFromRepo(type: UpdateType): Long {//returns seconds
        if (type == UpdateType.startup && !didDayChange(DateTime(defaults.getLastTimestamp()))) {
            return defaults.getLastTimestamp()
        }
        Timber.i("TranslatorsAndStylesController: update from repo, type=${type}")

        val version = BuildConfig.VERSION_NAME

        val fieldMap = mutableMapOf<String, String>()
        val stylesList = styles(type)
        if (!stylesList.isNullOrEmpty()) {
            val styleParameters = stylesList.map { style ->
                mapOf(
                    "id" to style.identifier,
                    "updated" to (style.updated.time / 1000L).toString(),
                    "url" to style.href
                )
            }

            fieldMap["styles"] = gson.toJson(styleParameters)
        }

        val networkResult = safeApiCall {
            nonZoteroApi.repoRequest(
                version = "${version}-android",
                timestamp = defaults.getLastTimestamp(),
                type = type.i,
                fieldMap = fieldMap
            )
        }
        if (networkResult is CustomResult.GeneralError.NetworkError) {
            throw Exception(networkResult.stringResponse)
        }
        networkResult as CustomResult.GeneralSuccess.NetworkSuccess
        val inputStream = networkResult.value!!.byteStream()
        val (timestamp, translators, styles) = parseRepoResponse(inputStream)
        syncRepoResponse(translators, styles)
        return timestamp
    }
    private fun parseRepoResponse(inputStream: InputStream):Triple<Long, List<Translator>,  List<Pair<String, String>>> {
        try {
            val parser = RepoParser.fromInputStream(inputStream = inputStream, gson = gson)
            parser.parseXml()
            Timber.i("TranslatorsAndStylesController: parsed ${parser.translators.size} translators and ${parser.styles.size} styles")
            return Triple(parser.timestamp, parser.translators, parser.styles)
        }catch (e: Exception) {
            Timber.e(e)
        }
        throw Error.cantParseXmlResponse
    }

    private fun splitTranslators(translators: List<Translator>): Pair<List<Translator>, List<Translator>> {
        val update = mutableListOf<Translator>()
        val delete = mutableListOf<Translator>()

        for (translator in translators) {
           val priority = translator.metadata["priority"] as? Int ?: continue
            if (priority > 0) {
                update.add(translator)
            } else {
                delete.add(translator)
            }
        }

        return update to delete
    }

    private fun metadata(translator: Translator): TranslatorMetadata {
        val id = translator.metadata["translatorID"] as? String ?: {
            Timber.e("TranslatorsAndStylesController: translator missing id - ${translator.metadata}")
            throw Error.translatorMissingId
        }
        val rawLastUpdated = translator.metadata["lastUpdated"] as? String ?: {
            Timber.e("TranslatorsAndStylesController: translator missing last updated - ${translator.metadata}")
            throw Error.translatorMissingLastUpdated
        }
        return TranslatorMetadata.from(
            id = id as String,
            filename = "",
            rawLastUpdated = rawLastUpdated as String
        )
    }

    private fun data(translator: Translator): ByteArray {
        var jsonMetadata: String

        try {
            jsonMetadata = gson.toJson(translator.metadata)
        } catch (error: Exception) {
            Timber.e(error, "TranslatorsAndStylesController: can't create data from metadata")
            throw Error.cantConvertTranslatorToData
        }

        val code = translator.code
        val newlines = "\n\n"

        jsonMetadata += (newlines)
        jsonMetadata += (code)
        return FileHelper.toByteArray(jsonMetadata)
    }

    private fun syncRepoResponse(
        translators: List<Translator>,
        styles: List<Pair<String, String>>
    ) {
        Timber.i("TranslatorsAndStylesController: sync repo response")

        // Split translators into deletions and updates, parse metadata.
        val (updateTranslators, deleteTranslators) = splitTranslators(translators = translators)
        Timber.i("TranslatorsAndStylesController: updateTranslators=${updateTranslators.size}; deleteTranslators=${deleteTranslators.size}")
        val updateTranslatorMetadata = updateTranslators.mapNotNull {
            try {
                metadata(it)
            } catch (e: Exception) {
                null
            }
        }
        val deleteTranslatorMetadata = deleteTranslators.mapNotNull {
            try {
                metadata(it)
            } catch (e: Exception) {
                null
            }
        }

        val (updateStyles, stylesData) = splitStyles(styles = styles)
        Timber.i("TranslatorsAndStylesController: updateStyles=${updateStyles.size}; remove local translators")

        // Remove local translators
        for (metadata in deleteTranslatorMetadata) {
            fileStore.translator(metadata.id).delete()
        }
        Timber.i("TranslatorsAndStylesController: write updated translators")
        for ((index, metadata) in updateTranslatorMetadata.withIndex()) {
            val data = data(updateTranslators[index])
            FileHelper.writeByteArrayToFile(fileStore.translator(filename = metadata.id), data)
        }
        Timber.i("TranslatorsAndStylesController: write updated styles")
        for ((filename, data) in stylesData) {
            FileHelper.writeByteArrayToFile(fileStore.style(filename), data)
        }

        Timber.i("TranslatorsAndStylesController: update db from repo")
        val repoRequest = SyncRepoResponseDbRequest(
            styles = updateStyles,
            translators = updateTranslatorMetadata,
            deleteTranslators = deleteTranslatorMetadata,
            fileStore = this.fileStore
        )
        bundleDbWrapper.realmDbStorage.perform(request = repoRequest)
    }


    private suspend fun styles(type: UpdateType): List<Style>? {
        if (type == UpdateType.shareExtension) {
            return null
        }

        try {
            var styles = emptyList<Style>()
            uiScope.launch {
                val rStyles = bundleDbWrapper.realmDbStorage.perform(
                    request = ReadStylesDbRequest(),
                    invalidateRealm = true
                )
                styles = rStyles.mapNotNull { Style.fromRStyle(it) }
            }.join()
            return styles
        } catch (e: Exception) {
            Timber.e(e, "TranslatorsAndStylesController: can't read styles")
            return null
        }
    }

    private fun didDayChange(date: DateTime): Boolean {
        val currentDate = DateTime.now()
        return currentDate.dayOfMonth != date.dayOfMonth
                || currentDate.monthOfYear != date.monthOfYear
                || currentDate.yearOfEra != date.yearOfEra
    }

    private fun checkFolderIntegrity(type: UpdateType)  {
        try {
            if (!fileStore.translatorItemsDirectory().exists()) {
                if (type != UpdateType.initial) {
                    Timber.e("TranslatorsAndStylesLoader: translators directory was missing!")
                }
                fileStore.translatorItemsDirectory().mkdirs()
            }

            if (type == UpdateType.initial) {
                return
            }

            val fileCount = fileStore.translatorItemsDirectory().listFiles()?.size ?: 0

            if (fileCount != 0) {
                return
            }

            defaults.setLastTimestamp(0L)
            defaults.setLastTranslatorCommitHash("")
            defaults.setLastTranslatorDeleted(0)
        } catch (error: Exception) {
            Timber.e(error, "TranslatorsAndStylesLoader: unable to restore folder integrity")
            throw error
        }

    }

    private fun process(error: Exception, updateType: UpdateType) {
        Timber.e(error, "TranslatorsAndStylesLoader: error")

        val isBundleLoadingError = (error as? Error)?.isBundleLoadingError == true
        if (!isBundleLoadingError) {
            return
        }
        //TODO show bundle load error dialog

    }
}