package org.zotero.android.locales

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pspdfkit.internal.utilities.toArrayList
import org.apache.commons.io.FileUtils
import org.zotero.android.files.FileStore
import org.zotero.android.ktx.unmarshalMap
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportLocaleReader @Inject constructor(
    private val fileStore: FileStore,
    private val gson: Gson,
) {
    sealed class Error : Exception() {
        object bundledFileMissing : Error()
    }

    fun loadIds(): List<String> {
        val localesUrl = File(fileStore.localesDirectory(), "locales.json")
        if (!localesUrl.exists()) {
            throw Error.bundledFileMissing
        }
        val localesJsonContent = FileUtils.readFileToString(localesUrl, "UTF-8")
        val dictionary = gson.fromJson(localesJsonContent, JsonObject::class.java)
        val element = dictionary["language-names"]
        val codes =
            element.unmarshalMap<String, List<String>>(gson) ?: return emptyList()
        return codes.keys.toArrayList()

    }
}