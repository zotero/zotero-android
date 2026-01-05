package org.zotero.android.sync

import android.content.Context
import io.realm.Realm
import io.realm.exceptions.RealmError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.logging.crash.CrashReporter
import org.zotero.android.architecture.logging.debug.DebugLogging
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.citation.CitationProcLoader
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.locales.CslLocalesLoader
import org.zotero.android.locales.ExportCslLocaleReader
import org.zotero.android.pdfworker.loader.PdfWorkerLoader
import org.zotero.android.screens.addbyidentifier.IdentifierLookupController
import org.zotero.android.screens.share.backgroundprocessor.BackgroundUploadProcessor
import org.zotero.android.translator.loader.TranslationLoader
import org.zotero.android.translator.loader.TranslatorsAndStylesLoader
import org.zotero.android.utilities.UtilitiesLoader
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsUserInitializedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Boolean>(applicationScope)
class Controllers @Inject constructor(
    dispatchers: Dispatchers,
    private val sessionDataEventStream: SessionDataEventStream,
    private val applicationScope: ApplicationScope,
    private val fileStore: FileStore,
    private val dbWrapperMain: DbWrapperMain,
    private val bundleDataDbWrapper: DbWrapperBundle,
    private val isUserInitializedEventStream: IsUserInitializedEventStream,
    private val sessionController: SessionController,
    private val userControllers: UserControllers,
    private val fileDownloader: AttachmentDownloader,
    private val backgroundUploadProcessor: BackgroundUploadProcessor,
    private val debugLogging: DebugLogging,
    private val crashReporter: CrashReporter,
    private val translatorsAndStylesLoader: TranslatorsAndStylesLoader,
    private val translationLoader: TranslationLoader,
    private val pdfWorkerLoader: PdfWorkerLoader,
    private val citationProcLoader: CitationProcLoader,
    private val utilitiesLoader: UtilitiesLoader,
    private val context: Context,
    private val identifierLookupController: IdentifierLookupController,
    private val cslLocalesLoader: CslLocalesLoader,
    private val defaults: Defaults,
    private val exportCslLocaleReader: ExportCslLocaleReader,
    ) {
    private var sessionCancellable: Job? = null
    private var apiKey: String? = null
    private var didInitialize: Boolean = false

    private var coroutineScope = CoroutineScope(dispatchers.io)

    fun init() {
        Realm.init(context)
        fileStore.init()
        debugLogging.startLoggingOnLaunchIfNeeded()
        createBundleDataDbStorage()
        crashReporter.processPendingReports()
        initializeSessionIfPossible()
        startApp()
        this.didInitialize = true
    }

    private fun startApp() {
        updateBundledItems()
        val controllers = this.userControllers
        val session = this.sessionDataEventStream.currentValue()
        if (session != null && controllers.isControllerInitialized) {
            controllers.enableSync(apiKey = session.apiToken)
        }
    }

    private fun updateBundledItems() {
        coroutineScope.launch {
            try {
                translationLoader.updateTranslationIfNeeded()
                translatorsAndStylesLoader.updateTranslatorItemsIfNeeded()
                pdfWorkerLoader.updatePdfWorkerIfNeeded()
                citationProcLoader.updateCitationProcIfNeeded()
                utilitiesLoader.updateUtilitiesIfNeeded()
                cslLocalesLoader.updateCslLocalesIfNeeded()
                setupExportDefaults()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update Translator or translation items")
            }
        }
    }
    private fun setupExportDefaults() {
        if (defaults.hasQuickCopyCslLocaleId()) {
            return
        }
        try {
            val localeIds = exportCslLocaleReader.loadIds()
            val l = Locale.getDefault().toLanguageTag()
            val defaultLocale = localeIds.firstOrNull { it.contains(l) } ?: "en-US"
            defaults.setQuickCopyCslLocaleId(defaultLocale)
            defaults.setExportLocaleId(defaultLocale)
        } catch (e: Exception) {
            Timber.e(e)
            return
        }
    }

    private fun initializeSessionIfPossible(failOnError: Boolean = false) {
        try {
            this.sessionController.initializeSession()
            update(data = sessionDataEventStream.currentValue(), isLogin = false)
            startObservingSession()
        } catch (error: Exception) {
            if (!failOnError) {
                Timber.e(error, "Controllers: session controller failed to initialize properly")
                initializeSessionIfPossible(failOnError = true)
                return
            }

            Timber.e(error, "Controllers: session controller failed to initialize properly")

            update(data = null, isLogin = false)
            startObservingSession()
        }
    }

    private fun startObservingSession() {
        this.sessionCancellable = sessionDataEventStream.flow()
            .drop(1)
            .onEach { data ->
                update(data = data, isLogin = true)
            }
            .launchIn(applicationScope)
    }

    private fun update(data: SessionData?, isLogin: Boolean) {
        if (data != null){
            set(data = data, isLogin = isLogin)
            this.apiKey = data.apiToken
        } else {
            clearSession()
            this.apiKey = null
        }
    }

    private fun set(data: SessionData, isLogin: Boolean) {
        try {
            userControllers.init(userId = data.userId)
            if (isLogin) {
                userControllers.enableSync(apiKey = data.apiToken)
            }
            isUserInitializedEventStream.emit(true)
        } catch (error: Throwable) {
            Timber.e(error, "Controllers: can't create UserControllers")

            this.sessionCancellable?.cancel()
            this.sessionCancellable = null
            this.sessionController.reset()
            startObservingSession()

            val realmError = error as? RealmError
            if (realmError != null) {
                dbWrapperMain.clearDatabaseFiles()
            }

            isUserInitializedEventStream.emit(false)
        }
    }

    private fun clearSession() {
        val controllers = this.userControllers
        controllers.disableSync(apiKey = this.apiKey)
        fileDownloader.stop()
        identifierLookupController.cancelAllLookups()
        backgroundUploadProcessor.cancelAllUploads()
        // TODO Cancel all background downloads

        FileHelper.deleteFolder(fileStore.cache())
        FileHelper.deleteFolder(fileStore.jsonCache)
        FileHelper.deleteFolder(fileStore.annotationPreviews)
        FileHelper.deleteFolder(fileStore.pageThumbnails)
        FileHelper.deleteFolder(fileStore.uploads)
        FileHelper.deleteFolder(fileStore.downloads)
        isUserInitializedEventStream.emit(false)
        if (dbWrapperMain.isInitialized) {
            dbWrapperMain.clearDatabaseFiles()
        }
    }

    fun willEnterForeground() {
        if (!this.didInitialize) {
            return
        }
        userControllers.maybeReconnectWebsockets()
//        startApp()
    }

    fun didEnterBackground() {
//        if(this.sessionDataEventStream.currentValue() != null) {
//            this.userControllers.disableSync(apiKey = null)
//        }
    }

    fun willTerminate() {
//        if(this.sessionDataEventStream.currentValue() != null) {
//            this.userControllers.disableSync(apiKey = null)
//        }
    }

    private fun createBundleDataDbStorage() {
        bundleDataDbWrapper.initBundleDataConfiguration()
    }
}
