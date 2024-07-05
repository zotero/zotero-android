package org.zotero.android.webdav

import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.StoreMtimeForAttachmentDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.webdav.data.WebDavError
import timber.log.Timber
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavController @Inject constructor(
    private val sessionStorage: WebDavSessionStorage,
    private val dbWrapper: DbWrapper,
) {

    private fun update(mtime: Long, key: String) {
        try {
            dbWrapper.realmDbStorage.perform(
                StoreMtimeForAttachmentDbRequest(
                    mtime = mtime,
                    key = key,
                    libraryId = LibraryIdentifier.custom(
                        RCustomLibraryType.myLibrary
                    )
                )
            )
        } catch (error: Exception) {
            Timber.e(error, "WebDavController: can't update mtime")
            throw error
        }
    }

    private fun remove(file: File) {
        file.delete()
    }

    val currentUrl: String?
        get() {
            try {
                return _createUrl()
            } catch (e: Exception) {
                //failing silently
            }
            return null
        }

    private fun createUrl(): String {
        val url = _createUrl()
        return url

    }

    private fun _createUrl(): String {
        val url = sessionStorage.url
        if (url.isEmpty()) {
            Timber.e("WebDavController: url not found")
            throw WebDavError.Verification.noUrl
        }

        val urlComponents = url.split("/")
        if (urlComponents.isEmpty()) {
            Timber.e("WebDavController: url components empty - $url")
            throw WebDavError.Verification.invalidUrl
        }

        val hostComponents = (urlComponents.firstOrNull() ?: "").split(":")
        val host = hostComponents.firstOrNull()
        val port = hostComponents.lastOrNull()?.toIntOrNull()

        val path: String
        if (urlComponents.size == 1) {
            path = "/zotero/"
        } else {
            path = "/" + urlComponents.drop(1).filter { it.isNotEmpty() }
                .joinToString(separator = "/") + "/zotero/"
        }

        try {
            val components = URL(sessionStorage.scheme.name, host, port!!, path)
            return components.toExternalForm()
        } catch (e: Exception) {
            Timber.e("WebDavController: could not create url from components. url=$url; host=${host ?: "missing"}; path=$path; port=${port?.toString() ?: "missing"}")
            throw WebDavError.Verification.invalidUrl
        }

    }

    private fun loadCredentials(): Pair<String, String> {
        val username = sessionStorage.username
        if (username.isEmpty()) {
            Timber.e("WebDavController: username not found")
            throw WebDavError.Verification.noUsername
        }
        val password = sessionStorage.password
        if (password.isEmpty()) {
            Timber.e("WebDavController: password not found")
            throw WebDavError.Verification.noPassword
        }
        return username to password
    }

    fun resetVerification() {
        this.sessionStorage.isVerified = false
    }
}