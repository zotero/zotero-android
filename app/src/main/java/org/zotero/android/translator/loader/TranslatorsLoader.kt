package org.zotero.android.translator.loader

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.zotero.android.api.BundleDataDb
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.SyncTranslatorsDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.Unzipper
import org.zotero.android.screens.share.data.TranslatorMetadata
import timber.log.Timber
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslatorsLoader @Inject constructor(
    dispatchers: Dispatchers,
    private val context: Context,
    private val gson: Gson,
    private val defaults: Defaults,
    private val unzipper: Unzipper,
    private val itemsUnzipper: TranslatorItemsUnzipper,
    private val fileStore: FileStore,
    @BundleDataDb
    private val bundleDbWrapper: DbWrapper
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

    fun translators(url: String): String {
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
                val firstQuotaIndex = str.indexOfFirst { it == '\'' }
                val lastQuotaIndex = str.indexOfLast { it == '\'' }
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
            rawString = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
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

    fun updateTranslatorIfNeeded() {
        if (defaults.shouldUpdateTranslator()) {
            unzipper.unzipStream(
                zipInputStream = context.assets.open("translator.zip"),
                location = fileStore.translatorDirectory().absolutePath
            )
            defaults.setShouldUpdateTranslator(false)
        }
    }

    private fun loadIndex(): List<TranslatorMetadata> {
        val inputStream = context.assets.open("translators/index.json")
        val type =
            TypeToken.getParameterized(MutableList::class.java, TranslatorMetadata::class.java).type
        return gson.fromJson(InputStreamReader(inputStream), type)
    }

    private suspend fun _updateTranslatorsFromBundle(forceUpdate: Boolean) {
        val hash = loadLastTranslatorCommitHash()
        Timber.i("TranslatorsLoader: should update translators from bundle, forceUpdate=$forceUpdate; oldHash=${defaults.getLastTranslatorCommitHash()}; newHash=$hash")
        if (!forceUpdate && defaults.getLastTranslatorCommitHash() == hash) {
            return
        }
        Timber.i("TranslatorsLoader: update translators from bundle")

        val (deletedVersion, deletedIndices) = loadDeleted()
        syncTranslatorsWithBundledData(deleteIndices = deletedIndices, forceUpdate = forceUpdate)

        defaults.setLastTranslatorDeleted(deletedVersion)
        defaults.setLastTranslatorCommitHash(hash)
    }

    private fun loadLastTranslatorCommitHash(): String {
        return loadFromBundle(resource = "translators/commit_hash.txt", map = { it })
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
        Timber.i("TranslatorsLoader: load index")
        val metadata = loadIndex()
        Timber.i("TranslatorsLoader: sync translators to database")
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

        Timber.i("TranslatorsLoader: updated $updated.size translators")
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
            val rawValue = IOUtils.toString(inputStream)
            return map(rawValue.trim().trim { it == '\n' })
        } catch (e: Exception) {
            Timber.e(e)
            throw Error.bundleMissing
        }
    }

    private suspend fun updateFromBundle() {
        try {
            _updateTranslatorsFromBundle(forceUpdate = false)
            val timestamp = loadLastTimestamp()
            if (timestamp > defaults.getLastTimestamp()) {
                defaults.setLastTimestamp(timestamp)
                return
            } else {
                return
            }
            
        } catch (error: Exception) {
            Timber.e(error, "TranslatorsLoader: can't update from bundle")
            throw Error.bundleLoading(error)
        }
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


        Timber.i("TranslatorsLoader: update translators and styles")
        try {
            checkFolderIntegrity(type = type)
            updateFromBundle()
            defaults.setLastTimestamp(System.currentTimeMillis() / 1000) //TODO
        } catch (error: Exception) {
            process(error = error, updateType = type)
        }
    }

    private fun checkFolderIntegrity(type: UpdateType)  {
        try {
            if (!fileStore.translatorItemsDirectory().exists()) {
                if (type != UpdateType.initial) {
                    Timber.e("TranslatorsLoader: translators directory was missing!")
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
            Timber.e(error, "TranslatorsLoader: unable to restore folder integrity")
            throw error
        }

    }

    private fun process(error: Exception, updateType: UpdateType) {
        Timber.e(error, "TranslatorsLoader: error")

        val isBundleLoadingError = (error as? Error)?.isBundleLoadingError == true
        if (!isBundleLoadingError) {
            return
        }
        //TODO show bundle load error dialog

    }
}