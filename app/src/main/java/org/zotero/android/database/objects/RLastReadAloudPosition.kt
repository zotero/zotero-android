package org.zotero.android.database.objects

import com.google.gson.JsonParser
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.kotlin.createObject
import org.zotero.android.api.pojo.sync.SettingKeyParser
import org.zotero.android.speech.data.ReadAloudResumePosition
import org.zotero.android.speech.data.SDTRect
import java.util.Date

enum class RLastReadAloudPositionChanges {
    position
}

open class RLastReadAloudPosition : RealmObject(), Updatable, Syncable, Deletable {
    @Index
    override var key: String = ""
    var pageIndex: Int? = null
    var rects: RealmList<RRect> = RealmList()
    var sourceJson: String? = null
    override var customLibraryKey: String? = null
    override var groupKey: Int? = null

    @Index
    override var version: Int = 0
    override lateinit var syncState: String
    override var lastSyncDate: Date? = null
    override var syncRetries: Int = 0
    override lateinit var changes: RealmList<RObjectChange>
    override lateinit var changeType: String //UpdatableChangeType
    override var deleted: Boolean = false

    override val isInvalidated: Boolean
        get() = !isValid

    val position: ReadAloudResumePosition?
        get() {
            val pageIndex = this.pageIndex
            if (pageIndex != null) {
                return ReadAloudResumePosition.Pdf(
                    pageIndex = pageIndex,
                    rects = rects.map { SDTRect(it.minX, it.minY, it.maxX - it.minX, it.maxY - it.minY) },
                )
            }
            val sourceJson = this.sourceJson ?: return null
            return try {
                ReadAloudResumePosition.Reader(JsonParser.parseString(sourceJson).asJsonObject)
            } catch (e: Exception) {
                null
            }
        }

    fun set(position: ReadAloudResumePosition, database: Realm) {
        if (rects.isValid) {
            rects.deleteAllFromRealm()
        }
        when (position) {
            is ReadAloudResumePosition.Pdf -> {
                pageIndex = position.pageIndex
                sourceJson = null
                for (rect in position.rects) {
                    val rRect = database.createObject<RRect>()
                    rRect.minX = rect.x
                    rRect.minY = rect.y
                    rRect.maxX = rect.x + rect.width
                    rRect.maxY = rect.y + rect.height
                    rects.add(rRect)
                }
            }

            is ReadAloudResumePosition.Reader -> {
                pageIndex = null
                sourceJson = position.source.toString()
            }
        }
    }

    fun matches(position: ReadAloudResumePosition): Boolean {
        return when (position) {
            is ReadAloudResumePosition.Pdf -> {
                pageIndex == position.pageIndex &&
                        rects.map { listOf(it.minX, it.minY, it.maxX, it.maxY) } ==
                        position.rects.map { listOf(it.x, it.y, it.x + it.width, it.y + it.height) }
            }

            is ReadAloudResumePosition.Reader -> {
                val stored = this.position as? ReadAloudResumePosition.Reader
                stored != null && stored.source == position.source
            }
        }
    }

    override val updateParameters: Map<String, Any>?
        get() {
            val libraryId = this.libraryId ?: return null
            val position = this.position ?: return null
            return mapOf(
                SettingKeyParser.uid(
                    key = key,
                    libraryId = libraryId,
                    prefix = "lastReadAloudPosition"
                ) to mapOf("value" to position.json)
            )
        }

    override fun markAsChanged(database: Realm) {
        changes.add(RObjectChange.create(changes = listOf(RLastReadAloudPositionChanges.position)))
        changeType = UpdatableChangeType.user.name
        deleted = false
        version = 0
    }

    override fun willRemove(database: Realm) {
        if (changes.isValid) {
            changes.deleteAllFromRealm()
        }
        if (rects.isValid) {
            rects.deleteAllFromRealm()
        }
    }
}