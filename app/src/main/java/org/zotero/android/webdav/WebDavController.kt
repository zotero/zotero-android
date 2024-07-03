package org.zotero.android.webdav

import org.zotero.android.webdav.data.WebDavError
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavController @Inject constructor(
    private val sessionStorage: WebDavSessionStorage
) {

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
}