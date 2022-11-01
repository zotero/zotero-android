package org.zotero.android.architecture.database.objects

import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import java.util.Date

data class Attachment(
    val type: Kind,
    val title: String,
    val key: String,
    val libraryId: LibraryIdentifier,
    val url: String? = null,
    val dateAdded: Date = Date(),
) {

    val id: String get() { return this.key }

    val location: FileLocation? get() {
        when (this.type) {
            is Kind.url -> return null
            is Kind.file -> return this.type.location
        }
    }

    enum class FileLocation {
        local, localAndChangedRemotely, remote, remoteMissing
    }

    enum class FileLinkType {
        importedUrl, importedFile, embeddedImage, linkedFile
    }

    sealed class Kind {
        data class file(val filename: String, val contentType: String, val location: FileLocation, val linkType: FileLinkType): Kind()
        data class url(val url: String): Kind()
    }

    fun changed(location: FileLocation, condition: (FileLocation) -> Boolean): Attachment? {
        when {
            this.type is Kind.file && condition(this.type.location) -> {
                return Attachment(type =  Kind.file(filename =  this.type.filename, contentType =  this.type.contentType, location = location, linkType = this.type.linkType),
                title =  this.title,
                url = this.url,
                dateAdded = this.dateAdded,
                key = this.key,
                libraryId = this.libraryId)
            }

            this.type is Kind.url || this.type is Kind.file ->
            return null
            else -> return null
        }
    }

    fun changed(location: FileLocation): Attachment? {
        when {
            this.type is Kind.file && (this.type.location != location) -> {
                return Attachment(type =  Kind.file(filename =  this.type.filename, contentType =  this.type.contentType, location = location, linkType = this.type.linkType),
                    title =  this.title,
                    url = this.url,
                    dateAdded = this.dateAdded,
                    key = this.key,
                    libraryId = this.libraryId)
            }

            this.type is Kind.url || this.type is Kind.file ->
                return null
            else -> return null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Attachment

        if (title != other.title) return false
        if (key != other.key) return false
        if (libraryId != other.libraryId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + libraryId.hashCode()
        return result
    }

    companion object {
        fun initWithItemAndKind(item: RItem, type: Kind): Attachment? {
            val libraryId = item.libraryId
            if (libraryId == null) {
                Timber.e("Attachment: library not assigned to item ${item.key}")
                return null
            }
            return Attachment(
                libraryId = libraryId,
                key = item.key,
                title = item.displayTitle,
                type = type,
                dateAdded = item.dateAdded,
                url = item.fields.firstOrNull { it.key == FieldKeys.Item.url }?.value,
            )
        }
    }
}
