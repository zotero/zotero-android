package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbRequest
import org.zotero.android.architecture.database.objects.AnnotationType
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.ObjectSyncState
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RItemChanges
import org.zotero.android.architecture.database.objects.RItemField
import org.zotero.android.architecture.database.objects.RObjectChange
import org.zotero.android.architecture.database.objects.RPath
import org.zotero.android.architecture.database.objects.RPathCoordinate
import org.zotero.android.architecture.database.objects.RRect
import org.zotero.android.architecture.database.objects.RTypedTag
import org.zotero.android.architecture.database.objects.UpdatableChangeType
import org.zotero.android.data.CGRect
import org.zotero.android.sync.AnnotationSplitter
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SplittablePathPoint

class SplitAnnotationsDbRequest(
    val keys:Set<String>,
    val libraryId:LibraryIdentifier,
):DbRequest {
    private data class Point(
        override val x:Double, override val y:Double
    ) : SplittablePathPoint

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val items = database.where<RItem>().keys(this.keys, this.libraryId).findAll()

        for (item in items) {
            split(item = item, database = database)
            item.willRemove(database)
            item.deleteFromRealm()

        }
    }

    private fun split(item:RItem, database: Realm) {
        val annotationType = item.fields.where().key(FieldKeys.Item.Annotation.type).findFirst()?.let { AnnotationType.valueOf(it.value) }
        if (annotationType == null) {
            return
        }

        when (annotationType) {
            AnnotationType.highlight -> {
                val rects = item.rects.map{ CGRect(x = it.minX, y = it.minY, width = (it.maxX - it.minY), height = (it.maxY - it.minY)) }

                val splitRects = AnnotationSplitter.splitRectsIfNeeded(rects = rects)
                if (splitRects == null) {
                    return
                }

                for (split in splitRects) {
                    createCopyWithoutPathsAndRects(item, database = database, additionalChange = { new ->
                            for (rect in split) {
                                val rRect = database.createEmbeddedObject(RRect::class.java, item, "rects")
                                rRect.minX = rect.minX
                                rRect.minY = rect.minY
                                rRect.maxX = rect.maxX
                                rRect.maxY = rect.maxY
                            }
                        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.rects)))
                    })
                }
            }


            AnnotationType.ink -> {
                val paths = points(item.paths)

                val splitPaths = AnnotationSplitter.splitPathsIfNeeded(paths = paths)
                if (splitPaths == null) {
                    return
                }

                for (split in splitPaths) {
                    createCopyWithoutPathsAndRects(item, database = database) { new ->
                        for((idx, path) in split.withIndex()){
                            val rPath = database.createEmbeddedObject(RPath::class.java, new, "paths")
                            rPath.sortIndex = idx

                            for ((idy, coordinate) in path.withIndex()) {
                                val rXCoordinate = database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                                rXCoordinate.value = coordinate.x
                                rXCoordinate.sortIndex = idy * 2

                                val rYCoordinate = database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                                rYCoordinate.value = coordinate.y
                                rYCoordinate.sortIndex = (idy * 2) + 1
                            }
                        }
                        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.paths)))
                    }
                }
            }
            else -> {
                //no-op
            }
        }
    }

    private fun points(paths:List<RPath>): List<List<Point>> {
        var points: MutableList<List<Point>> = mutableListOf()

        for (path in paths.sortedBy { it.sortIndex }) {
            val sortedCoordinates = path.coordinates.sortedBy { it.sortIndex }
            var coordinates = mutableListOf<Point>()

            for (idx in 0 until (sortedCoordinates.size / 2)) {
                val xCoord = sortedCoordinates[idx * 2]
                val yCoord = sortedCoordinates[(idx * 2) + 1]
                coordinates.add(Point(x = xCoord.value, y = yCoord.value))
            }

            points.add(coordinates)
        }
        return points
    }

    private fun createCopyWithoutPathsAndRects(item: RItem, database: Realm, additionalChange: (RItem) -> Unit) {
        var new = database.createObject<RItem>()
        new.key = KeyGenerator.newKey
        new.rawType = item.rawType
        new.localizedType = item.localizedType
        new.dateAdded = item.dateAdded
        new.dateModified = item.dateModified
        new.libraryId = item.libraryId
        new.deleted = item.deleted
        new.syncState = ObjectSyncState.synced.name
        new.changeType = UpdatableChangeType.syncResponse.name
        val changes: List<RItemChanges> = listOf(RItemChanges.parent, RItemChanges.fields, RItemChanges.type, RItemChanges.tags)
        new.changes.add(RObjectChange.create(changes = changes))

        new.parent = item.parent
        new.createdBy = item.createdBy
        new.lastModifiedBy = item.lastModifiedBy

        for (tag in item.tags!!) {
            val newTag = database.createObject<RTypedTag>()
            newTag.type = tag.type
            newTag.item = new
            newTag.tag = tag.tag
        }

        for (field in item.fields) {
            val newField = database.createEmbeddedObject(RItemField::class.java, new, "fields")
            newField.key = field.key
            newField.baseKey = field.baseKey
            newField.value = field.value
            newField.changed = true
        }

        additionalChange(new)
    }


}