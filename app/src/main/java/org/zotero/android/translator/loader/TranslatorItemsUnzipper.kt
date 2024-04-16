package org.zotero.android.translator.loader

import android.content.Context
import org.zotero.android.files.FileStore
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

private val BUFFER_SIZE = 8192

@Singleton
class TranslatorItemsUnzipper @Inject constructor(private val context: Context, private val fileStore: FileStore) {

    @Throws(Exception::class)
    fun unzip(translators: List<Pair<String, String>>) {
        var size: Int
        val buffer = ByteArray(BUFFER_SIZE)
        val zin = ZipInputStream(BufferedInputStream(context.assets.open("translators/translators.zip"), BUFFER_SIZE))
        try {
            var ze: ZipEntry? = null
            while (zin.nextEntry.also { ze = it } != null) {
                val translatorPair = translators.find { it.second == ze!!.name }
                if(translatorPair == null) {
                    continue
                }

                val unzipFile = fileStore.translator(translatorPair.first)
                if (!ze!!.isDirectory) {
                    // check for and create parent directories if they don't exist
                    val parentDir: File? = unzipFile.getParentFile()
                    if (null != parentDir) {
                        if (!parentDir.isDirectory()) {
                            parentDir.mkdirs()
                        }
                    }

                    // unzip the file
                    val out: FileOutputStream = FileOutputStream(unzipFile, false)
                    val fout = BufferedOutputStream(out, BUFFER_SIZE)
                    try {
                        while (zin.read(buffer, 0, BUFFER_SIZE).also { size = it } != -1) {
                            fout.write(buffer, 0, size)
                        }
                        zin.closeEntry()
                    } finally {
                        fout.flush()
                        fout.close()
                    }
                }
            }
        } finally {
            zin.close()
        }
    }
}