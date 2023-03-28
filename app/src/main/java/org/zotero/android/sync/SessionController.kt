package org.zotero.android.sync

import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.core.StateEventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.files.FileStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionController @Inject constructor(
    private val defaults: Defaults,
    private val fileStore: FileStore,
    private val sessionDataEventStream: SessionDataEventStream
) {

    val isLoggedIn: Boolean
        get() {
            return this.sessionDataEventStream.currentValue() != null
        }
    private var isInitialized: Boolean = false

    fun initializeSession() {
        val apiToken = this.defaults.getApiToken()
        val userId = this.defaults.getUserId()

        this.isInitialized = true

        val token = apiToken
        if (token != null && userId > 0L) {
            this.sessionDataEventStream.emit(SessionData(userId, token))
        } else {
            this.sessionDataEventStream.emit(null)
        }
    }

    fun register(userId: Long, username: String, displayName: String, apiToken: String) {
        this.defaults.setUserId(userId)
        this.defaults.setUsername(username)
        this.defaults.setDisplayName(displayName)
        this.defaults.setApiToken(apiToken)
        this.sessionDataEventStream.emit(SessionData(userId, apiToken))
        this.isInitialized = true
    }

    fun reset() {
        this.defaults.reset()
        this.fileStore.reset()
        this.sessionDataEventStream.emit(null)
    }
}

@Singleton
class SessionDataEventStream @Inject constructor(applicationScope: ApplicationScope) :
    StateEventStream<SessionData?>(applicationScope, null)

data class SessionData(val userId: Long, val apiToken: String)