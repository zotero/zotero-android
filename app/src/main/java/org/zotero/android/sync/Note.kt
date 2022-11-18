package org.zotero.android.sync

import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.requests.key
import timber.log.Timber

class Note(
    val key: String,
    var title: String,
    var text: String,
    var tags: List<Tag>
) {
    val id: String get() { return this.key }

    constructor(key: String, text: String, tags: List<Tag>): this(key = key, title = NotePreviewGenerator.preview(text) ?: text, text = text, tags = tags)


    companion object {

        fun init(item: RItem): Note? {
            if (item.rawType != ItemTypes.note) {
                Timber.e("Trying to create Note from RItem which is not a note!")
                return null
            }
            return Note(
                key = item.key,
                title = item.displayTitle,
                text = item.fields.where().key(FieldKeys.Item.note).findFirst()?.value ?: "",
                tags = item.tags?.map { Tag(it) }?.toList() ?: emptyList()
            )

        }
    }

}
