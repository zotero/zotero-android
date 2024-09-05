package org.zotero.android.sync.syncactions.architecture

import dagger.hilt.EntryPoints
import org.zotero.android.ZoteroApplication

abstract class SyncAction {

    val bindings by lazy {
        EntryPoints.get(ZoteroApplication.instance, SyncActionDependencyProvider::class.java)
    }

    val zoteroApi by lazy {
        bindings.zoteroApi()
    }

    val settingsResponseMapper by lazy {
        bindings.settingsResponseMapper()
    }

    val dbWrapperMain by lazy {
        bindings.dbWrapperMain()
    }

    val gson by lazy {
        bindings.gson()
    }

    val backgroundUploaderContext by lazy {
        bindings.backgroundUploaderContext()
    }

    val fileStore by lazy {
        bindings.fileStore()
    }

    val schemaController by lazy {
        bindings.schemaController()
    }

    val dateParser by lazy {
        bindings.dateParser()
    }

    val itemResponseMapper by lazy {
        bindings.itemResponseMapper()
    }

    val collectionResponseMapper by lazy {
        bindings.collectionResponseMapper()
    }
    val searchResponseMapper by lazy {
        bindings.searchResponseMapper()
    }

    val dispatcher by lazy {
        bindings.dispatcher()
    }

    val attachmentDownloader by lazy {
        bindings.attachmentDownloader()
    }

    val attachmentDownloaderEventStream by lazy {
        bindings.attachmentDownloaderEventStream()
    }

    val nonZoteroApi by lazy {
        bindings.nonZoteroApi()
    }

    val updatesResponseMapper by lazy {
        bindings.updatesResponseMapper()
    }

    val pageIndexResponseMapper by lazy {
        bindings.pageIndexResponseMapper()
    }

    val webDavController by lazy {
        bindings.webDavController()
    }

    val sessionStorage by lazy {
        bindings.webDavSessionStorage()
    }

    val defaults by lazy {
        bindings.defaults()
    }
}
