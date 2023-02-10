
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCustomLibrary
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RVersions

class InitializeCustomLibrariesDbRequest : DbResponseRequest<Boolean> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): Boolean {
        val existingRecord = database
            .where<RCustomLibrary>()
            .findAll()
            .filter { it.type == RCustomLibraryType.myLibrary.name }
        if (existingRecord.isNotEmpty()) {
            return false
        }
        val library = database.createObject<RCustomLibrary>(RCustomLibraryType.myLibrary.name)
        library.orderId = 1
        database.createEmbeddedObject(RVersions::class.java, library, "versions")
        return true
    }
}
