package org.zotero.android.sync
import android.graphics.PointF
import android.graphics.RectF
import com.google.gson.JsonObject
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.FreeTextAnnotation
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.annotations.TextMarkupAnnotation
import com.pspdfkit.annotations.UnderlineAnnotation
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.helpers.formatter.iso8601WithFractionalSeconds
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.data.PDFDocumentAnnotation

class AnnotationConverterV2 {
    enum class Kind {
        export,
        zotero,
    }

    companion object {

        fun annotation(
            data: JsonObject,
            author: String,
            isAuthor: Boolean,
        ): PDFDocumentAnnotation? {
            val type = (data["type"]?.asString)?.let{AnnotationType.valueOf(it)} ?: return null

            if (!AnnotationsConfig.supportedV2.contains(type)) {
                return null
            }
            val key = data["id"]?.asString ?: return null
            val pageLabel = data["pageLabel"]?.asString   ?: return null
            val comment = data["comment"]?.asString?.let { it.trim().trim { it == '\n' } } ?: ""
            val sortIndex = data["sortIndex"]?.asString ?: return null
            val dateAdded = (data["dateCreated"]?.asString)?.let {
                iso8601WithFractionalSeconds.parse(it)
            } ?: return null
            val dateModified =
                (data["dateModified"]?.asString)?.let {
                    iso8601WithFractionalSeconds.parse(it)
                } ?: return null

            println()
            //

            //            val color = data["color"]?.asString ?: return@mapNotNull null
            //
            //            val position = data["position"]?.asJsonObject ?: return@mapNotNull null
            //            val text = data["text"]?.asString ?: return@mapNotNull null
            //            val type = (data["type"]?.asString)?.let{AnnotationType.valueOf(it)} ?: return@mapNotNull null
            //            val rawTags = data["tags"]?.asJsonArray ?: return@mapNotNull null
            //            val tags = rawTags.mapNotNull { dataAsJson ->
            //                val data = dataAsJson.asJsonObject
            //                val name = data["name"]?.asString ?: return@mapNotNull null
            //                val color = data["color"]?.asString ?: return@mapNotNull null
            //                Tag(name = name, color = color)
            //            }

            return null

//            val rects: List<RectF>
//            var text: String? = null
//            val paths: List<List<PointF>>
//            var lineWidth: Float? = null
//            var fontSize: Float? = null
//            var rotation: Int? = null
//
//            val noteAnnotation = annotation as? NoteAnnotation
//            val highlightAnnotation = annotation as? HighlightAnnotation
//            val squareAnnotation = annotation as? SquareAnnotation
//            val inkAnnotation = annotation as? InkAnnotation
//            val underlineAnnotation = annotation as? UnderlineAnnotation
//            val freeTextAnnotation = annotation as? FreeTextAnnotation
//            if (noteAnnotation != null) {
//                type = AnnotationType.note
//                rects = rects(noteAnnotation)
//                paths = emptyList()
//            } else if (highlightAnnotation != null) {
//                type = AnnotationType.highlight
//                rects = rectsUnderlineAndHightlight(highlightAnnotation)
//                text = highlightAnnotation.highlightedText
//                paths = emptyList()
//            } else if (squareAnnotation != null) {
//                type = AnnotationType.image
//                rects = rects(squareAnnotation)
//                paths = emptyList()
//            } else if (inkAnnotation != null) {
//                type = AnnotationType.ink
//                rects = emptyList()
//                paths = paths(inkAnnotation)
//                lineWidth = inkAnnotation.lineWidth
//            } else if (underlineAnnotation != null) {
//                type = AnnotationType.underline
//                rects = rectsUnderlineAndHightlight(underlineAnnotation)
//                text = underlineAnnotation.highlightedText
//                paths = emptyList()
//            } else if (freeTextAnnotation != null) {
//                type = AnnotationType.text
//                fontSize = annotation.textSize
//                rotation = annotation.rotation
//                paths = emptyList()
//                rects = rects(freeTextAnnotation)
//            }
//
//            else {
//                return null
//            }
//
//            return PDFDocumentAnnotation(
//                key = key,
//                type = type,
//                page = page,
//                pageLabel = pageLabel,
//                rects = rects,
//                paths = paths,
//                lineWidth = lineWidth,
//                color = color,
//                comment = comment,
//                text = text,
//                fontSize = fontSize,
//                rotation = rotation,
//                dateAdded = ItemsSortType.Field.dateAdded,
//                dateModified = ItemsSortType.Field.dateModified,
//                sortIndex = sortIndex,
//                author = author,
//                isAuthor = isAuthor,
//                isZoteroAnnotation = annotation.isZoteroAnnotation
//            )
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
        private fun rectsUnderlineAndHightlight(highlightAndUnderlineAnnotation: TextMarkupAnnotation) : List<RectF> {
            return (highlightAndUnderlineAnnotation.rects ?: listOf(highlightAndUnderlineAnnotation.boundingBox))
        }
        private fun rects(annotation: SquareAnnotation) : List<RectF>  {
            return listOf(annotation.boundingBox)
        }

        private fun rects(annotation: FreeTextAnnotation) : List<RectF>  {
            return listOf(annotation.boundingBox)
//            if (annotation.rotation <= 0) {
//                return listOf(
//                    annotation.boundingBox
//                )
//            }
//
//            val tempAnnotation = FreeTextAnnotation(
//                annotation.pageIndex,
//                annotation.boundingBox,
//                annotation.contents
//            )
//            tempAnnotation.textSize = annotation.textSize
//            tempAnnotation.fontName = annotation.fontName
//            tempAnnotation.setRotation(annotation.rotation)
//
//            val originalRotation = tempAnnotation.rotation
//            val oldBox = tempAnnotation.boundingBox
//            println(oldBox)
//            tempAnnotation.setRotation(0)
//            val boundingBox = tempAnnotation.boundingBox
////            tempAnnotation.setRotation(originalRotation)
//            return listOf(boundingBox)
        }

        fun paths(annotation: InkAnnotation): List<List<PointF>> {
            return annotation.lines.map { lines ->
                lines.map { group ->
                    group.rounded(3)
                }
            }
        }

        fun rects(annotation: Annotation): List<RectF>? {
            when(annotation) {
                is NoteAnnotation -> {
                    rects(annotation)
                }
                is HighlightAnnotation, is UnderlineAnnotation -> {
                    rectsUnderlineAndHightlight(annotation as TextMarkupAnnotation)
                }
                is SquareAnnotation -> {
                    rects(annotation)
                }
                is FreeTextAnnotation -> {
                    return rects(annotation)
                }
            }
            return null
        }
    }
}
