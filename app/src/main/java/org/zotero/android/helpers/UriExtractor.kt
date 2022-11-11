package org.zotero.android.helpers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import org.zotero.android.files.FileStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriExtractor @Inject constructor(private val mContext: Context, private val mFileStore: FileStore) {
    fun extractFilePathFromUri(uri: Uri, shouldCopyToTempFile: Boolean): String {
        try {
            val filePath = getFilePath(uri, shouldCopyToTempFile)
            if (filePath == null || filePath.isEmpty()) {
                throw UriMediaExtractorException("Path extracted from URI is null or empty")
            }
            return filePath
        } catch (extractorException: UriMediaExtractorException) {
            throw extractorException
        } catch (e: Exception) {
            Log.e(
                UriExtractor::class.java.getName(),
                "Failed to get path from uri again $uri",
                e
            )
            throw UriMediaExtractorException("Unable to load selected media", e)
        }
    }

    @Throws(IOException::class, UriMediaExtractorException::class)
    private fun getFilePath(uri: Uri, shouldCopyToTempFile: Boolean): String? {
        var uri = uri
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        // deal with different Uris.
        if (DocumentsContract.isDocumentUri(mContext, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                uri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                )
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("image" == type) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                selection = "_id=?"
                selectionArgs = arrayOf(split[1])
            }
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            var cursor: Cursor? = null
            try {
                val contentResolver = contentResolver
                cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
                val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(column_index)
                    if (path != null) {
                        return path
                    }
                }
            } catch (e: Exception) {
            }
            cursor?.close()
        } else if ("file".equals(uri.scheme, ignoreCase = true) || uri.toString()
                .startsWith("/storage")
        ) {
            return uri.path
        }
        return if (shouldCopyToTempFile) {
            copyUriToTempFile(uri)
        } else null
    }

    @Throws(IOException::class, UriMediaExtractorException::class)
    private fun copyUriToTempFile(uri: Uri): String {
        val fileExtension = queryUriFileExtension(uri)
            ?: throw UriMediaExtractorException("File extension is null")
        val tempFile: File = File(
            mFileStore.getCachesDirectory(),
            "selectedCloudFileCopy.$fileExtension"
        )
        downloadMediaFromCloud(uri, tempFile)
        copyExifDataFromUri(uri, tempFile)
        return tempFile.path
    }

    @Throws(IOException::class, UriMediaExtractorException::class)
    private fun downloadMediaFromCloud(uri: Uri, tempFile: File) {
        var `is`: InputStream? = null
        var os: OutputStream? = null
        try {
            `is` = contentResolver.openInputStream(uri)
            os = FileOutputStream(tempFile)
            ByteStreams.copy(`is`, os)
        } catch (e: Exception) {
            throw UriMediaExtractorException("Unable to download media from cloud", e)
        } finally {
            `is`?.close()
            os?.close()
        }
    }

    private val imageProjection = arrayOf(
        MediaStore.Video.Media.DATE_TAKEN,
        MediaStore.Images.Media.LATITUDE,
        MediaStore.Images.Media.LONGITUDE
    )

    @SuppressLint("RestrictedApi")
    @Throws(IOException::class)
    private fun copyExifDataFromUri(uri: Uri, resultFile: File) {
        if (!shouldCopyExifMetadata(resultFile.path)) {
            return
        }
        val cursor = contentResolver.query(uri, imageProjection, null, null, null)
        if (cursor == null || cursor.count == 0) {
            return
        }
        val dateTakenColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
        val latitudeColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.LATITUDE)
        val longitudeColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
        cursor.moveToNext()
        val exifNew = ExifInterface(resultFile.absolutePath)
        if (dateTakenColumnIndex != -1) {
            val dateTaken = cursor.getLong(dateTakenColumnIndex)
            resultFile.setLastModified(dateTaken)
            exifNew.dateTime = dateTaken
        }
        if (latitudeColumnIndex != -1 && longitudeColumnIndex != -1) {
            exifNew.setLatLong(
                cursor.getDouble(latitudeColumnIndex),
                cursor.getDouble(longitudeColumnIndex)
            )
        }
        exifNew.saveAttributes()
        cursor.close()
    }

    private fun shouldCopyExifMetadata(filePath: String): Boolean {
        return filePath.endsWith("jpg")
    }

    private val singleFileProjection = arrayOf(OpenableColumns.DISPLAY_NAME)
    private fun queryUriFileExtension(uri: Uri): String? {
        val cursor = contentResolver.query(uri, singleFileProjection, null, null, null)
        if (cursor == null || cursor.count == 0) {
            return null
        }
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex == -1) {
            return null
        }
        cursor.moveToFirst()
        val fileName = cursor.getString(nameIndex)
        cursor.close()
        return Files.getFileExtension(fileName)
    }

    private val contentResolver: ContentResolver
        private get() = mContext.contentResolver

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}