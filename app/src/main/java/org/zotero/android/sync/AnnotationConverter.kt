package org.zotero.android.sync
import android.graphics.PointF
import android.graphics.RectF
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.BorderStyle
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.document.PdfDocument
import io.realm.RealmResults
import org.json.JSONObject
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.database.objects.RItem
import org.zotero.android.ktx.key
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.AnnotationBoundingBoxConverter
import org.zotero.android.pdf.AnnotationEditability
import org.zotero.android.pdf.DatabaseAnnotation
import org.zotero.android.pdf.DocumentAnnotation
import timber.log.Timber
import java.util.Date
import java.util.EnumSet
import kotlin.math.max
import kotlin.math.roundToInt

class AnnotationConverter {
    enum class Kind {
        export,
        zotero,
    }

    companion object {
        fun annotations(
            items: RealmResults<RItem>,
            type: Kind = Kind.zotero,
            currentUserId: Long,
            library: Library,
            displayName: String,
            username: String,
            boundingBoxConverter: AnnotationBoundingBoxConverter,
            isDarkMode: Boolean,
        ): List<Annotation> {
            return items.map { item ->
                annotation(
                    zoteroAnnotation = DatabaseAnnotation(item = item),
                    type = type,
                    currentUserId = currentUserId,
                    library = library,
                    displayName = displayName,
                    username = username,
                    boundingBoxConverter = boundingBoxConverter,
                    isDarkMode = isDarkMode
                )
            }
        }

        fun annotation(
            zoteroAnnotation: DatabaseAnnotation,
            type: Kind,
            currentUserId: Long,
            library: Library,
            displayName: String,
            username: String,
            isDarkMode: Boolean,
            boundingBoxConverter: AnnotationBoundingBoxConverter
        ): Annotation {
            val (color, alpha, blendMode) = AnnotationColorGenerator.color(
                zoteroAnnotation.color,
                isHighlight = (zoteroAnnotation.type == AnnotationType.highlight),
                isDarkMode = isDarkMode
            )
            val annotation: Annotation
            when (zoteroAnnotation.type) {
                AnnotationType.image -> annotation = areaAnnotation(
                    zoteroAnnotation,
                    type = type,
                    color = color,
                    boundingBoxConverter = boundingBoxConverter
                )

                AnnotationType.highlight -> annotation = highlightAnnotation(
                    zoteroAnnotation,
                    type = type,
                    color = color,
                    alpha = alpha,
                    boundingBoxConverter = boundingBoxConverter
                )

                AnnotationType.note -> annotation = noteAnnotation(
                    zoteroAnnotation,
                    type = type,
                    color = color,
                    boundingBoxConverter = boundingBoxConverter
                )

                AnnotationType.ink -> annotation = inkAnnotation(
                    zoteroAnnotation,
                    type = type,
                    color = color,
                    boundingBoxConverter = boundingBoxConverter
                )
            }

            when (type) {
                Kind.export -> {
                    annotation.customData = null
                }

                Kind.zotero -> {
                    annotation.customData =
                        JSONObject().put(AnnotationsConfig.keyKey, zoteroAnnotation.key)

                    if (zoteroAnnotation.editability(
                            currentUserId = currentUserId,
                            library = library
                        ) != AnnotationEditability.editable
                    ) {
                        annotation.flags =
                            EnumSet.copyOf(annotation.flags + AnnotationFlags.READONLY)
                    }
                }
            }
            if (blendMode != null) {
                annotation.blendMode = blendMode
            }

            annotation.contents = zoteroAnnotation.comment
            annotation.creator =
                zoteroAnnotation.author(displayName = displayName, username = username)
            annotation.name = "Zotero-${zoteroAnnotation.key}"
            return annotation
        }

        private fun areaAnnotation(
            annotation: org.zotero.android.pdf.Annotation,
            type: Kind,
            color: Int,
            boundingBoxConverter: AnnotationBoundingBoxConverter
        ): SquareAnnotation {
            val square: SquareAnnotation
            val boundingBox =
                annotation.boundingBox(boundingBoxConverter = boundingBoxConverter).rounded(3)
            when (type) {
                Kind.export -> {
                    square = SquareAnnotation(
                        annotation.page,
                        boundingBox
                    )
                }

                Kind.zotero -> {
                    square = ZoteroSquareAnnotations(
                        annotation.page,
                        boundingBox
                    )

                }
            }
            square.borderColor = color
            square.borderWidth = AnnotationsConfig.imageAnnotationLineWidth

            return square
        }

        private fun highlightAnnotation(
            annotation: org.zotero.android.pdf.Annotation,
            type: Kind,
            color: Int,
            alpha: Float,
            boundingBoxConverter: AnnotationBoundingBoxConverter
        ): HighlightAnnotation {
            val highlight : HighlightAnnotation
            val rects =
                annotation.rects(boundingBoxConverter = boundingBoxConverter).map { it.rounded(3) }
            when (type) {
                Kind.export -> {
                    highlight = HighlightAnnotation(
                        annotation.page,
                        rects
                    )
                }
                Kind.zotero -> {
                    highlight = ZoteroHighlightAnnotation(
                        annotation.page,
                        rects
                    )
                }
            }
            highlight.boundingBox = annotation.boundingBox(boundingBoxConverter = boundingBoxConverter).rounded( 3)
            highlight.color = color
            highlight.alpha = alpha

            return highlight
        }

        private fun noteAnnotation(
            annotation: org.zotero.android.pdf.Annotation,
            type: Kind,
            color: Int,
            boundingBoxConverter: AnnotationBoundingBoxConverter
        ): NoteAnnotation {
            val boundingBox0 =
                annotation.boundingBox(boundingBoxConverter = boundingBoxConverter).rounded(3)
            val boundingBox =
                RectF(
                    /* left = */ boundingBox0.left,
                    /* top = */ boundingBox0.bottom + AnnotationsConfig.noteAnnotationSize.second,
                    /* right = */ boundingBox0.left + AnnotationsConfig.noteAnnotationSize.first,
                    /* bottom = */ boundingBox0.bottom
                )
            val note: NoteAnnotation
            when (type) {
                Kind.export -> {
                    note = NoteAnnotation(annotation.page, boundingBox, annotation.comment, null)
                }

                Kind.zotero -> {
                    note =
                        ZoteroNoteAnnotation(annotation.page, boundingBox, annotation.comment)
                }
            }

            note.borderStyle = BorderStyle.DASHED
            note.color = color
            return note
        }

        private fun inkAnnotation(
            annotation: org.zotero.android.pdf.Annotation,
            type: Kind, color: Int, boundingBoxConverter: AnnotationBoundingBoxConverter): InkAnnotation {
            val lines = annotation.paths(boundingBoxConverter = boundingBoxConverter)
            val ink = InkAnnotation(annotation.page)
            ink.lines = lines
            ink.color = color
            ink.lineWidth = annotation.lineWidth ?: 1F
            return ink
        }

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
            val isAuthor = annotation.creator == displayName || annotation.creator == username
            val comment = annotation.contents?.let { it.trim().trim { it == '\n' } } ?: ""
            val sortIndex = sortIndex(annotation, boundingBoxConverter = boundingBoxConverter)
            val date = Date()

            val author: String
            if (isAuthor) {
                author = createName(displayName, username = username)
            } else {
                author = annotation.creator ?: "Unknown"
            }

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
                dateModified = date,
                sortIndex = sortIndex,
                author = author,
                isAuthor = isAuthor,
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
        fun sortIndex(annotation: Annotation, boundingBoxConverter: AnnotationBoundingBoxConverter?):  String {
            val rect: RectF
            if (annotation is HighlightAnnotation) {
                rect = annotation.rects.firstOrNull() ?: annotation.boundingBox
            } else {
                rect = annotation.boundingBox
            }

            val textOffset = boundingBoxConverter?.textOffset(rect = rect, page = annotation.pageIndex) ?: 0
            val minY = boundingBoxConverter?.sortIndexMinY(rect = rect, page = annotation.pageIndex)?.roundToInt() ?: 0
            if (minY < 0) {
                Timber.w("AnnotationConverter: annotation ${annotation.key} has negative y position ${minY}")
            }
            return sortIndex(pageIndex = annotation.pageIndex, textOffset = textOffset, minY = minY)
        }

        fun sortIndex(pageIndex: Int, textOffset: Int, minY: Int): String {
            return String.format("%05d|%06d|%05d", pageIndex, textOffset, max(0, minY))
        }

        private fun createName(displayName: String, username: String): String {
            if (!displayName.isEmpty()) {
                return displayName
            }
            if (!username.isEmpty()) {
                return username
            }
            return "Unknown"
        }

    }
}
