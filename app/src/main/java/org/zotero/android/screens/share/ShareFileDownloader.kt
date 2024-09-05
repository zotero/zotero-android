package org.zotero.android.screens.share

import com.google.common.io.ByteProcessor
import com.google.common.io.ByteStreams
import com.google.common.io.Closeables
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareFileDownloader @Inject constructor(
    private val nonZoteroApi: NonZoteroApi
) {

    suspend fun download(
        url: String,
        file: File,
        cookies: String?,
        userAgent: String?,
        referrer: String?,
        updateProgressBar: (progress: Int) -> Unit,
    ) {
        val headers: MutableMap<String, String> = LinkedHashMap()
        if (userAgent != null) {
            headers["User-Agent"] = userAgent
        }
        if (referrer != null) {
            headers["Referer"] = referrer
        }
        if (cookies != null) {
            headers["Cookie"] = cookies
        }
        val networkResult = safeApiCall {
            nonZoteroApi.downloadFileStreaming(url = url, headers = headers)
        }
        when (networkResult) {
            is CustomResult.GeneralSuccess -> {
                val byteStream = networkResult.value!!.byteStream()
                val total = networkResult.value!!.contentLength()
                var progress = 0L
                val out = FileOutputStream(file);
                try {
                    ByteStreams.readBytes(byteStream,
                        object : ByteProcessor<Void?> {
                            @Throws(IOException::class)
                            override fun processBytes(
                                buffer: ByteArray,
                                offset: Int,
                                length: Int
                            ): Boolean {
                                out.write(buffer, offset, length)
                                progress += length
                                val progressResult = (progress / total.toDouble() * 100).toInt()
                                if (progressResult > 0) {
                                    println()
                                }
                                updateProgressBar(progressResult)
                                return true
                            }

                            override fun getResult(): Void? {
                                return null
                            }
                        })
                } catch (e: Exception) {
                    Timber.e(e, "Could not download $url")
                    throw e
                } finally {
                    Closeables.close(out, true)
                }
            }

            is CustomResult.GeneralError.CodeError -> {
                throw networkResult.throwable
            }

            is CustomResult.GeneralError.NetworkError -> {
                throw Exception(networkResult.stringResponse)
            }
        }
    }

}