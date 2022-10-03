
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RCustomLibrary
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.architecture.database.objects.RVersions
import kotlin.reflect.KClass

class InitializeCustomLibrariesDbRequest: DbResponseRequest<Boolean, Boolean> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm, clazz: KClass<Boolean>?): Boolean {
        val existingRecord = database.where<RCustomLibrary>().findAll().filter { it.type == RCustomLibraryType.myLibrary.name }
        if (existingRecord.isNotEmpty()) {
            return false
        }
        database.executeTransaction {
            val library = RCustomLibrary()
            library.type = RCustomLibraryType.myLibrary.name
            library.orderId = 1
            library.versions = RVersions()
            database.insertOrUpdate(library)
        }

        return true
    }


}
