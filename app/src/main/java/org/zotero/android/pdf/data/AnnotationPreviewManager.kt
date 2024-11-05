package org.zotero.android.pdf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.graphics.scale
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.FreeTextAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.configuration.rendering.PageRenderConfiguration
import com.pspdfkit.document.PdfDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.ktx.isZoteroAnnotation
import org.zotero.android.ktx.previewId
import org.zotero.android.ktx.shouldRenderPreview
import org.zotero.android.pdf.cache.AnnotationPreviewMemoryCache
import org.zotero.android.sync.LibraryIdentifier
import java.io.FileOutputStream
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationPreviewManager @Inject constructor(
    dispatchers: Dispatchers,
    private val fileStore: FileStore,
    private val memoryCache: AnnotationPreviewMemoryCache,
    private val context: Context,
) {
    private val currentlyProcessingAnnotations = Collections.synchronizedSet(mutableSetOf<String>())
    private var coroutineScope = CoroutineScope(dispatchers.default)

    fun store(
        rawDocument: PdfDocument,
        annotation: Annotation,
        parentKey: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean,
        annotationMaxSideSize: Int,
    ) {
        if (!annotation.shouldRenderPreview || !annotation.isZoteroAnnotation || !annotation.isAttached) {
            return
        }
        enqueue(
            annotation = annotation,
            key = annotation.previewId,
            parentKey = parentKey,
            libraryId = libraryId,
            isDark = isDark,
            bitmapSize = annotationMaxSideSize,
            rawDocument = rawDocument,
        )
    }

    fun delete(annotation: Annotation, parentKey: String, libraryId: LibraryIdentifier) {
        if (!annotation.shouldRenderPreview || !annotation.isZoteroAnnotation) {
            return
        }

        val key = annotation.previewId
        fileStore.annotationPreview(
            annotationKey = key,
            pdfKey = parentKey,
            libraryId = libraryId,
            isDark = true
        ).delete()
        fileStore.annotationPreview(
            annotationKey = key,
            pdfKey = parentKey,
            libraryId = libraryId,
            isDark = false
        ).delete()
    }

    fun deleteAll(parentKey: String, libraryId: LibraryIdentifier) {
        fileStore.annotationPreviews(pdfKey = parentKey, libraryId = libraryId).deleteRecursively()
    }

    fun hasPreview(
        key: String,
        parentKey: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean
    ): Boolean {
        return fileStore.annotationPreview(
            annotationKey = key,
            pdfKey = parentKey,
            libraryId = libraryId,
            isDark = isDark
        ).exists()
    }

    private fun enqueue(
        rawDocument: PdfDocument,
        annotation: Annotation,
        key: String,
        parentKey: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean = false,
        bitmapSize: Int
    ) = coroutineScope.launch {
        if (currentlyProcessingAnnotations.contains(key)) {
            return@launch
        }
        currentlyProcessingAnnotations.add(key)

        val resultBitmap: Bitmap = generateRawDocumentBitmap(
            annotation = annotation,
            rawDocument = rawDocument,
            maxSide = bitmapSize
        )

        val shouldDrawAnnotation = annotation is InkAnnotation || annotation is FreeTextAnnotation
        if (shouldDrawAnnotation) {
            drawAnnotationOnBitmap(resultBitmap, annotation)
        }
        completeRequest(
            bitmap = resultBitmap,
            key = key,
            parentKey = parentKey,
            libraryId = libraryId,
            isDark = isDark
        )
        currentlyProcessingAnnotations.remove(key)
    }

    private fun drawAnnotationOnBitmap(
        sourceBitmap: Bitmap,
        annotation: Annotation
    ) {
        val annotationBitmap = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        annotation.renderToBitmap(annotationBitmap)
        val canvas = Canvas(sourceBitmap)
        canvas.drawBitmap(
            annotationBitmap,
            null,
            Rect(0, 0, sourceBitmap.width, sourceBitmap.height),
            null
        )
        annotationBitmap.recycle()
    }

    private fun generateRawDocumentBitmap(
        annotation: Annotation,
        rawDocument: PdfDocument,
        maxSide: Int,
    ): Bitmap {
        val annotationRect = annotation.boundingBox
        val width = (annotationRect.right - annotationRect.left).toInt()
        val height = (annotationRect.top - annotationRect.bottom).toInt()

        val documentSize = rawDocument.getPageSize(annotation.pageIndex).toRect()
        val renderConfig = PageRenderConfiguration.Builder()
            .region(
                (-annotationRect.left).toInt(),
                (-(documentSize.height() - annotationRect.top)).toInt(),
                documentSize.width().toInt(),
                documentSize.height().toInt()
            )
            .build()

        val rawDocumentBitmap: Bitmap = rawDocument.renderPageToBitmap(
            context,
            annotation.pageIndex,
            width,
            height,
            renderConfig
        )
        val scaleX = width / maxSide.toDouble()
        val scaleY = height / maxSide.toDouble()
        val resultScale = scaleX.coerceAtLeast(scaleY)
        val resultVideoViewWidth = (width / resultScale).toInt()
        val resultVideoViewHeight = (height / resultScale).toInt()
        return rawDocumentBitmap.scale(resultVideoViewWidth, resultVideoViewHeight, true)
    }

    private fun completeRequest(
        bitmap: Bitmap,
        key: String,
        parentKey: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean
    ) {
        cache(
            bitmap = bitmap,
            key = key,
            pdfKey = parentKey,
            libraryId = libraryId,
            isDark = isDark
        )
        memoryCache.addToCache(key, bitmap)
    }

    private fun cache(
        bitmap: Bitmap,
        key: String,
        pdfKey: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean
    ) {
        val tempFile = fileStore.generateTempFile()
        val fileStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileStream)
        fileStream.close()

        val finalFile = fileStore.annotationPreview(
            annotationKey = key,
            pdfKey = pdfKey,
            libraryId = libraryId,
            isDark = isDark
        )
        tempFile.renameTo(finalFile)
    }

    fun cancelProcessing() {
        currentlyProcessingAnnotations.clear()
        coroutineScope.cancel()
    }
}