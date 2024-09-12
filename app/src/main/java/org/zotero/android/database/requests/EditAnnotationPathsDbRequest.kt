package org.zotero.android.database.requests

import android.graphics.PointF
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RPath
import org.zotero.android.database.objects.RPathCoordinate
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.pdf.data.AnnotationBoundingBoxConverter
import org.zotero.android.pdf.data.PDFDatabaseAnnotation
import org.zotero.android.sync.LibraryIdentifier

class EditAnnotationPathsDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val paths: List<List<PointF>>,
    private val boundingBoxConverter: AnnotationBoundingBoxConverter,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return
        val annotation = PDFDatabaseAnnotation.init(item) ?: return
        val page = annotation.page
        val dbPaths = this.paths.map { path ->
            path.map { this.boundingBoxConverter.convertToDb(point = it, page = page) ?: it }
        }
        if(!paths(dbPaths, item.paths)) { return }
        sync(paths = dbPaths, item, database = database)
    }

    private fun sync(paths: List<List<PointF>>, item: RItem, database: Realm) {
        for (path in item.paths) {
            path.coordinates.deleteAllFromRealm()
        }
        item.paths.deleteAllFromRealm()

        paths.forEachIndexed { idx, path ->
            val rPath = database.createEmbeddedObject(RPath::class.java, item, "paths")
            rPath.sortIndex = idx
            path.forEachIndexed { idy, point ->
                val rXCoordinate = database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                rXCoordinate.value = point.x.toDouble()
                rXCoordinate.sortIndex = idy * 2

                val rYCoordinate = database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                rYCoordinate.value = point.y.toDouble()
                rYCoordinate.sortIndex = (idy * 2) + 1
            }

        }

        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.paths)))
        item.changeType = UpdatableChangeType.user.name
    }


    private fun paths(paths:List<List<PointF>>, itemPaths: List<RPath>): Boolean {
        if (paths.size != itemPaths.size) {
            return true
        }
        val sortedPaths = itemPaths.sortedBy { it.sortIndex }

        for (idx in paths.indices) {
            val path = paths[idx]
            val itemPath = sortedPaths[idx]

            if ((path.size * 2) != itemPath.coordinates.size) {
                return true
            }
            val sortedCoordinates = itemPath.coordinates.sort("sortIndex")

            path.forEachIndexed { idy, point ->
                if (point.x.toDouble() != sortedCoordinates[idy * 2]?.value || point.y.toDouble() != sortedCoordinates[(idy * 2) + 1]?.value) {
                    return true
                }
            }
        }

        return false
    }

}