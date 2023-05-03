package org.zotero.android.sync
import android.graphics.PointF
import android.graphics.RectF
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.document.PdfDocument
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.ktx.key
import org.zotero.android.pdf.AnnotationBoundingBoxConverter
import org.zotero.android.pdf.DocumentAnnotation
import java.util.Date

class AnnotationConverter {
    companion object {
        fun annotation(
            document: PdfDocument,
            annotation: Annotation,
            color: String,
            library: Library,
            username: String,
            displayName: String,
            boundingBoxConverter: AnnotationBoundingBoxConverter?
        ): DocumentAnnotation? {
            if (!AnnotationsConfig.supported.contains(annotation.type)) {
                return null
            }
            val key = annotation.key ?: annotation.uuid
            val page = annotation.pageIndex
            val pageLabel =
                document.getPageLabel(annotation.pageIndex, false) ?: "${annotation.pageIndex + 1}"
            val comment = annotation.contents?.let { it.trim().trim { it == '\n' } } ?: ""
            val date = Date()

            val type: AnnotationType
            val rects: List<RectF>
            val text: String?
            val paths: List<List<PointF>>
            val lineWidth: Float?

            val noteAnnotation = annotation as? NoteAnnotation
            val highlightAnnotation = annotation as? HighlightAnnotation
            val squareAnnotation = annotation as? SquareAnnotation
            val inkAnnotation = annotation as? InkAnnotation
            if (noteAnnotation != null) {
                type = AnnotationType.note
                rects = rects(noteAnnotation)
                text = null
                paths = emptyList()
                lineWidth = null
            } else if (highlightAnnotation != null) {
                type = AnnotationType.highlight
                rects = rects(highlightAnnotation)
                //TODO removeNewLines
                text = highlightAnnotation.highlightedText
                paths = emptyList()
                lineWidth = null
            } else if (squareAnnotation != null) {
                type = AnnotationType.image
                rects = rects(squareAnnotation)
                text = null
                paths = emptyList()
                lineWidth = null
            } else if (inkAnnotation != null) {
                type = AnnotationType.ink
                rects = emptyList()
                text = null
                paths = paths(inkAnnotation)
                lineWidth = inkAnnotation.lineWidth
            } else {
                return null
            }

            return DocumentAnnotation(
                key = key,
                type = type,
                page = page,
                pageLabel = pageLabel,
                rects = rects,
                paths = paths,
                lineWidth = lineWidth,
                color = color,
                comment = comment,
                text = text,
                dateModified = date
            )
        }

        private fun rects(annotation: NoteAnnotation): List<RectF> {
            return listOf(
                RectF(
                    /* left = */
                    annotation.boundingBox.left,
                    /* top = */
                    annotation.boundingBox.bottom + AnnotationsConfig.noteAnnotationSize.first,
                    /* right = */
                    annotation.boundingBox.left + AnnotationsConfig.noteAnnotationSize.second,
                    /* bottom = */
                    annotation.boundingBox.bottom,
                )
            )
        }
        private fun rects(annotation: HighlightAnnotation) : List<RectF> {
            return (annotation.rects ?: listOf(annotation.boundingBox))
        }
        private fun rects(annotation: SquareAnnotation) : List<RectF>  {
            return listOf(annotation.boundingBox)
        }

        private fun paths(annotation: InkAnnotation): List<List<PointF>> {
            return annotation.lines ?: emptyList()
        }

    }
}
