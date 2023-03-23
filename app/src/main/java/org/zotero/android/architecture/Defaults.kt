package org.zotero.android.architecture

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.files.DataMarshaller
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class Defaults @Inject constructor(
    private val context: Context,
    private val dataMarshaller: DataMarshaller,
) {
    private val sharedPrefsFile = "ZoteroPrefs"
    private val userId = "userId"
    private val name = "name"
    private val username = "username"
    private val displayName = "displayName"
    private val apiToken = "apiToken"
    private val webDavPassword = "webDavPassword"
    private val showSubcollectionItems = "showSubcollectionItems"
    private val selectedLibrary = "selectedLibrary"
    private val selectedCollectionId = "selectedCollectionId"
    private val lastUsedCreatorNamePresentation = "LastUsedCreatorNamePresentation"
    private val itemsSortType = "ItemsSortType"
    private val showCollectionItemCounts = "showCollectionItemCounts"

    val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(
            sharedPrefsFile,
            Context.MODE_PRIVATE
        )
    }

    fun setUserId(str: Long) {
        sharedPreferences.edit { putLong(userId, str) }
    }

    fun setName(str: String) {
        sharedPreferences.edit { putString(name, str) }
    }

    fun setUsername(str: String) {
        sharedPreferences.edit { putString(username, str) }
    }

    fun setDisplayName(str: String) {
        sharedPreferences.edit { putString(displayName, str) }
    }

    fun setApiToken(str: String?) {
        sharedPreferences.edit { putString(apiToken, str) }
    }

    fun getApiToken(): String? {
        return sharedPreferences.getString(apiToken, null )
    }

    fun setWebDavPassword(str: String?) {
        sharedPreferences.edit { putString(webDavPassword, str) }
    }

    fun getWebDavPassword(): String? {
        return sharedPreferences.getString(webDavPassword, null )
    }

    fun isUserLoggedIn() :Boolean {
        return getApiToken() != null
    }

    fun getUserId(): Long {
        return sharedPreferences.getLong(userId, 0L)
    }

    fun showSubcollectionItems(): Boolean {
        return sharedPreferences.getBoolean(showSubcollectionItems, false)
    }

    fun setShowSubcollectionItems(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(showSubcollectionItems, newValue) }
    }

    fun getSelectedLibrary(): LibraryIdentifier {
        val json: String = sharedPreferences.getString(
            selectedLibrary,
            null
        )
            ?: return LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
        return dataMarshaller.unmarshal(json)
    }

    fun setSelectedLibrary(
        libraryIdentifier: LibraryIdentifier,
    ) {
        val json = dataMarshaller.marshal(libraryIdentifier)
        sharedPreferences.edit { putString(selectedLibrary, json) }
    }

    fun getSelectedCollectionId(): CollectionIdentifier {
        val json: String = sharedPreferences.getString(
            selectedCollectionId,
            null
        )
            ?: return CollectionIdentifier.custom(CollectionIdentifier.CustomType.all)
        return dataMarshaller.unmarshal(json)
    }

    fun setSelectedCollectionId(
        collectionIdentifier: CollectionIdentifier,
    ) {
        val json = dataMarshaller.marshal(collectionIdentifier)
        sharedPreferences.edit { putString(selectedCollectionId, json) }
    }

    fun setCreatorNamePresentation(namePresentation: ItemDetailCreator.NamePresentation) {
        val json = dataMarshaller.marshal(namePresentation)
        sharedPreferences.edit { putString(lastUsedCreatorNamePresentation, json) }
    }

    fun getCreatorNamePresentation(): ItemDetailCreator.NamePresentation {
        val json: String = sharedPreferences.getString(
            lastUsedCreatorNamePresentation,
            null
        )
            ?: return ItemDetailCreator.NamePresentation.separate
        return dataMarshaller.unmarshal(json)
    }

    fun setItemsSortType(sortType: ItemsSortType) {
        val json = dataMarshaller.marshal(sortType)
        sharedPreferences.edit { putString(itemsSortType, json) }
    }

    fun getItemsSortType(): ItemsSortType {
        val json: String = sharedPreferences.getString(
            itemsSortType,
            null
        )
            ?: return ItemsSortType.default
        return dataMarshaller.unmarshal(json)
    }

    fun showCollectionItemCounts(): Boolean {
        return sharedPreferences.getBoolean(showCollectionItemCounts, true)
    }

    fun setShowCollectionItemCounts(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(showCollectionItemCounts, newValue) }
    }

    fun reset() {
        setUsername("")
        setDisplayName("")
        setUserId(0L)
        setSelectedLibrary(LibraryIdentifier.custom(RCustomLibraryType.myLibrary))
        setSelectedCollectionId(CollectionIdentifier.custom(CollectionIdentifier.CustomType.all))
        setShowSubcollectionItems(false)
        setApiToken(null)
        setWebDavPassword(null)
    }

}
