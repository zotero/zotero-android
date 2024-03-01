package org.zotero.android.translator.loader

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.commons.io.IOUtils
import timber.log.Timber
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslatorsLoader @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    private val uuidExpression: Pattern?
        get() {
            return try {
                Pattern.compile("setTranslator\\(['\"](?<uuid>[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12})['\"]\\)")
            } catch (e: Exception) {
                Timber.e(e, "can't create parts expression")
                null
            }
        }

    fun translators(url: String): String {
        val translators = loadTranslators(url)
        val translatorsJson = gson.toJson(translators)
        return translatorsJson
    }

    private fun loadTranslators(url: String?): List<Map<String, Any>> {
        try {
            val loadedUuids = mutableSetOf<String>()
            val allUuids = context.assets.list("translator/translator_items")!!
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
                loadRawTranslator(context.assets.open("translator/translator_items/$uuid"), url) ?: continue
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
            matches.add(m.group("uuid")!!)
        }
        return matches.toSet()
    }

    private fun loadRawTranslator(
        fileInputStream: InputStream,
        url: String? = null
    ): Map<String, Any>? {
        val rawString: String
        try {
            rawString = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8)
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
}