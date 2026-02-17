package org.zotero.android.screens.htmlepub.reader.data

import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.data.AnnotationEditability
import org.zotero.android.sync.Library
import org.zotero.android.sync.Tag
import java.util.Date

interface ReaderAnnotation {
    val key: String
    val type: AnnotationType
    val pageLabel: String
    val lineWidth: Float?
    val comment: String
    val color: String
    val text: String?
    val fontSize: Float?
    val sortIndex: String
    val dateAdded: Date
    val dateModified: Date
    val tags: List<Tag>

    fun author(displayName: String, username: String): String
    fun isAuthor(currentUserId: Long): Boolean
    fun editability(currentUserId: Long, library: Library): AnnotationEditability

    val displayColor: String get(){
        if (!color.startsWith("#")) {
            return "#" + this.color
        }
        return this.color
    }
}