package org.zotero.android.helpers

import android.content.Context
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidFileExtensions @Inject constructor(private val context: Context) {

    private val list: Set<String> by lazy {
        val inputStream = context.assets.open("ValidFileExtensions.txt")
        val rawValue = IOUtils.toString(inputStream)
        rawValue.split('\n').toSet()
    }

    fun hasValidExtension(str: String): Boolean {
        val ext = FilenameUtils.getExtension(str)
        if (ext.isNullOrBlank()) {
            return false
        }
        return list.contains(".$ext")
    }

}