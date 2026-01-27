package org.zotero.android.screens.htmlepub.reader.data
import com.google.gson.JsonObject
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.data.AnnotationEditability
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import java.util.Date

data class HtmlEpubAnnotation(
    override val key: String,
    override val type: AnnotationType,
    override val pageLabel: String,
    val position: JsonObject,
    val author: String,
    val isAuthor: Boolean,
    override val color: String,
    override val comment: String,
    override val text: String?,
    override val sortIndex: String,
    override val dateAdded: Date,
    override val dateModified: Date,
    override val tags: List<Tag>,
): ReaderAnnotation {

    fun copy(comment: String): HtmlEpubAnnotation {
        return HtmlEpubAnnotation(
            key = key,
            type = type,
            pageLabel = pageLabel,
            position = position,
            author = author,
            isAuthor = isAuthor,
            color = color,
            comment = comment,
            text = text,
            sortIndex = sortIndex,
            dateAdded = dateAdded,
            dateModified = dateModified,
            tags = tags
        )
    }

    override val lineWidth: Float
        get() = 0f

    override val fontSize: Float
        get() = 12f

    override fun author(displayName: String, username: String): String {
        return author
    }

    override fun isAuthor(currentUserId: Long): Boolean {
        return isAuthor
    }

    override fun editability(currentUserId: Long, library: Library): AnnotationEditability {
        when (library.identifier) {
            is LibraryIdentifier.custom -> {
                return if (library.metadataEditable) AnnotationEditability.editable else AnnotationEditability.notEditable
            }

            is LibraryIdentifier.group -> {
                if (!library.metadataEditable) {
                    return AnnotationEditability.notEditable
                }
                return if (isAuthor) AnnotationEditability.editable else AnnotationEditability.deletable
            }
        }
    }


}
