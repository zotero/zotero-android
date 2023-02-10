package org.zotero.android.sync

import io.realm.exceptions.RealmError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.apache.commons.io.FileUtils
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.database.DbWrapper
import org.zotero.android.files.FileStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsUserInitializedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Boolean>(applicationScope)
class Controllers @Inject constructor(
    private val sessionDataEventStream: SessionDataEventStream,
    private val applicationScope: ApplicationScope,
    private val fileStore: FileStore,
    private val dbWrapper: DbWrapper,
    private val isUserInitializedEventStream: IsUserInitializedEventStream,
    private val sessionController: SessionController,
    private val userControllers: UserControllers,
    private val fileDownloader: AttachmentDownloader,
    ) {
    private var sessionCancellable: Job? = null
    private var apiKey: String? = null
    private var didInitialize: Boolean = false

    fun init() {
        fileStore.init()
        initializeSessionIfPossible()
        startApp()
        this.didInitialize = true
    }

    private fun startApp() {
        val controllers = this.userControllers
        val session = this.sessionDataEventStream.currentValue()
        if (session != null && controllers.isControllerInitialized) {
            controllers.enableSync(apiKey = session.apiToken)
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
            userControllers.init(userId = data.userId, controllers = this)
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
                dbWrapper.clearDatabaseFiles()
            }

            isUserInitializedEventStream.emit(false)
        }
    }

    private fun clearSession() {
        val controllers = this.userControllers
        controllers.disableSync(apiKey = this.apiKey)
        fileDownloader.stop()
        // TODO Cancel all background uploads

        FileUtils.deleteDirectory(fileStore.cache())
        FileUtils.deleteDirectory(fileStore.jsonCache)
        FileUtils.deleteDirectory(fileStore.annotationPreviews)
        FileUtils.deleteDirectory(fileStore.uploads)
        FileUtils.deleteDirectory(fileStore.downloads)
        isUserInitializedEventStream.emit(false)
        if (dbWrapper.isInitialized) {
            dbWrapper.clearDatabaseFiles()
        }
    }

    fun willEnterForeground() {
        if (!this.didInitialize) {
            return
        }
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

}
