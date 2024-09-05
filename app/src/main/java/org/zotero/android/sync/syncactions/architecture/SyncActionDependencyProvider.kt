package org.zotero.android.sync.syncactions.architecture

import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.mappers.CollectionResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.PageIndexResponseMapper
import org.zotero.android.api.mappers.SearchResponseMapper
import org.zotero.android.api.mappers.SettingsResponseMapper
import org.zotero.android.api.mappers.UpdatesResponseMapper
import org.zotero.android.architecture.Defaults
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.attachmentdownloader.AttachmentDownloaderEventStream
import org.zotero.android.backgrounduploader.BackgroundUploaderContext
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.files.FileStore
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.SchemaController
import org.zotero.android.webdav.WebDavController
import org.zotero.android.webdav.WebDavSessionStorage

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncActionDependencyProvider {
    fun zoteroApi(): ZoteroApi
    fun settingsResponseMapper(): SettingsResponseMapper
    fun dbWrapperMain(): DbWrapperMain
    fun gson(): Gson
    fun backgroundUploaderContext(): BackgroundUploaderContext
    fun fileStore(): FileStore
    fun schemaController(): SchemaController
    fun dateParser(): DateParser
    fun itemResponseMapper(): ItemResponseMapper
    fun collectionResponseMapper(): CollectionResponseMapper
    fun searchResponseMapper(): SearchResponseMapper
    fun dispatcher(): CoroutineDispatcher
    fun attachmentDownloader(): AttachmentDownloader
    fun attachmentDownloaderEventStream(): AttachmentDownloaderEventStream
    fun nonZoteroApi(): NonZoteroApi
    fun updatesResponseMapper(): UpdatesResponseMapper
    fun pageIndexResponseMapper(): PageIndexResponseMapper
    fun webDavController(): WebDavController
    fun webDavSessionStorage(): WebDavSessionStorage
    fun defaults(): Defaults

}