package org.zotero.android.sync.syncactions.architecture

import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import org.zotero.android.api.NoAuthenticationApi
import org.zotero.android.api.SyncApi
import org.zotero.android.api.mappers.CollectionResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.PageIndexResponseMapper
import org.zotero.android.api.mappers.SearchResponseMapper
import org.zotero.android.api.mappers.SettingsResponseMapper
import org.zotero.android.api.mappers.UpdatesResponseMapper
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.attachmentdownloader.AttachmentDownloaderEventStream
import org.zotero.android.backgrounduploader.BackgroundUploaderContext
import org.zotero.android.database.DbWrapper
import org.zotero.android.files.FileStore
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.SchemaController

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncActionDependencyProvider {
    fun syncApi(): SyncApi
    fun settingsResponseMapper(): SettingsResponseMapper
    fun dbWrapper(): DbWrapper
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
    fun noAuthenticationApi(): NoAuthenticationApi
    fun updatesResponseMapper(): UpdatesResponseMapper
    fun pageIndexResponseMapper(): PageIndexResponseMapper

}