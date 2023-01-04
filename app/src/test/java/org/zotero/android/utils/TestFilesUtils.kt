package org.zotero.android.utils

import com.google.common.io.CharStreams
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets

object TestFilesUtils {

    fun getTestAssetInputStream(assetPath: String): InputStream {
        return javaClass.classLoader!!.getResourceAsStream(assetPath)
    }

    fun getAssetUri(assetPath: String): URI {
        return javaClass.classLoader!!.getResource(assetPath).toURI()
    }

    fun loadTextFile(assetPath: String): String {
        return CharStreams.toString(InputStreamReader(getTestAssetInputStream(assetPath), StandardCharsets.UTF_8))
    }

}
