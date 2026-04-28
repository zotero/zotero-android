package org.zotero.android.sync
import android.graphics.PointF
import android.graphics.RectF
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
            lineWidthFromUser: Float? = null,
        ): PDFDocumentAnnotation? {
            val type = (data["type"]?.asString)?.let { AnnotationType.valueOf(it) } ?: return null
            if (!AnnotationsConfig.supportedV2.contains(type)) {
                return null
            }
            val key = data["id"]?.asString ?: return null
            val pageLabel = data["pageLabel"]?.asString ?: return null
            val comment = data["comment"]?.asString?.let { it.trim().trim { it == '\n' } } ?: ""
            val sortIndex = data["sortIndex"]?.asString ?: return null
            val dateAdded = (data["dateCreated"]?.asString)?.let {
                iso8601WithFractionalSeconds.parse(it)
            } ?: return null
            val dateModified =
                (data["dateModified"]?.asString)?.let {
                    iso8601WithFractionalSeconds.parse(it)
                } ?: return null
            val color = data["color"]?.asString ?: return null
            val position = data["position"]?.asJsonObject ?: return null
            val page = position["pageIndex"]?.asInt ?: 0

            val rects: List<RectF>
            var text: String? = null
            val paths: List<List<PointF>>
            var lineWidth: Float? = null
            var fontSize: Float? = null
            var rotation: Int? = null

            when (type) {
                AnnotationType.ink -> {
                    rects = emptyList()
                    paths = pathsForInk(position["paths"].asJsonArray)
                    lineWidth = lineWidthFromUser!!
                }

                AnnotationType.image -> {
                    rects = rectsForSquare(position["rects"].asJsonArray)
                    paths = emptyList()
                }

                AnnotationType.note -> {
                    rects = rectsForNote(position["rects"].asJsonArray)
                    paths = emptyList()
                }

                AnnotationType.underline -> {
                    rects = rectsUnderlineAndHightlight(position["rects"].asJsonArray)
                    text = data["text"]?.asString
                    paths = emptyList()
                }

                AnnotationType.highlight -> {
                    rects = rectsUnderlineAndHightlight(position["rects"].asJsonArray)
                    text = data["text"]?.asString
                    paths = emptyList()
                }

                AnnotationType.text -> {
                    fontSize = position["fontSize"].asFloat
                    rotation = position["rotation"].asInt
                    paths = emptyList()
                    rects = rectsForText(position["rects"].asJsonArray)
                }
            }
            return PDFDocumentAnnotation(
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
                fontSize = fontSize,
                rotation = rotation,
                dateAdded = dateAdded,
                dateModified = dateModified,
                sortIndex = sortIndex,
                author = author,
                isAuthor = isAuthor,
                isZoteroAnnotation = true
            )
        }

        private fun rectsForNote(annotation: JsonArray): List<RectF> {
            val rectsJsonArray = annotation.asJsonArray[0].asJsonArray
            return listOf( RectF(
                /* left = */ rectsJsonArray[0].asFloat,
                /* top = */ rectsJsonArray[1].asFloat,
                /* right = */ rectsJsonArray[2].asFloat,
                /* bottom = */ rectsJsonArray[3].asFloat
            ))
        }
        private fun rectsUnderlineAndHightlight(annotation: JsonArray) : List<RectF> {
            return annotation.asJsonArray.map {
                val rectsJsonArray = it.asJsonArray
                RectF(
                    /* left = */ rectsJsonArray[0].asFloat,
                    /* top = */ rectsJsonArray[1].asFloat,
                    /* right = */ rectsJsonArray[2].asFloat,
                    /* bottom = */ rectsJsonArray[3].asFloat
                )
            }
        }
        private fun rectsForSquare(annotation: JsonArray) : List<RectF>  {
            val rectsJsonArray = annotation.asJsonArray[0].asJsonArray
            return listOf( RectF(
                /* left = */ rectsJsonArray[0].asFloat,
                /* top = */ rectsJsonArray[1].asFloat,
                /* right = */ rectsJsonArray[2].asFloat,
                /* bottom = */ rectsJsonArray[3].asFloat
            ))
        }

        private fun rectsForText(annotation: JsonArray) : List<RectF>  {
            val rectsJsonArray = annotation.asJsonArray[0].asJsonArray
            return listOf( RectF(
                /* left = */ rectsJsonArray[0].asFloat,
                /* top = */ rectsJsonArray[1].asFloat,
                /* right = */ rectsJsonArray[2].asFloat,
                /* bottom = */ rectsJsonArray[3].asFloat
            ))
        }

        fun pathsForInk(outerArray: JsonArray): List<List<PointF>> {
            return outerArray.map { lines ->
                lines.asJsonArray.chunked(2).map { coordinatePair ->
                    val (x, y) = coordinatePair
                    PointF(
                        x.asFloat.rounded(3),
                        y.asFloat.rounded(3)
                    )
                }
            }
        }

//        fun rects(annotation: Annotation): List<RectF>? {
//            when(annotation) {
//                is NoteAnnotation -> {
//                    rects(annotation)
//                }
//                is HighlightAnnotation, is UnderlineAnnotation -> {
//                    rectsUnderlineAndHightlight(annotation as TextMarkupAnnotation)
//                }
//                is SquareAnnotation -> {
//                    rects(annotation)
//                }
//                is FreeTextAnnotation -> {
//                    return rects(annotation)
//                }
//            }
//            return null
//        }
    }
}
