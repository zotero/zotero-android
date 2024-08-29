package org.zotero.android.webdav

import org.zotero.android.architecture.Defaults
import org.zotero.android.webdav.data.WebDavScheme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavSessionStorage @Inject constructor(
    private val defaults: Defaults
) {
    var isEnabled: Boolean
        get() {
            return defaults.isWebDavEnabled()
        }
        set(newValue) {
            defaults.setWebDavEnabled(newValue)
        }

    var isVerified: Boolean
        get() {
            return defaults.isWebDavVerified()
        }
        set(newValue) {
            defaults.setWebDavVerified(newValue)
        }

    var username: String
        get() {
            return defaults.getWebDavUsername() ?: ""
        }
        set(newValue) {
            defaults.setWebDavUsername(newValue.ifEmpty { null })
        }

    var url: String
        get() {
            return defaults.getWebDavUrl() ?: ""
        }
        set(newValue) {
            defaults.setWebDavUrl(newValue.ifEmpty { null })
        }

    var scheme: WebDavScheme
        get() {
            return defaults.getWebDavScheme()
        }
        set(newValue) {
            defaults.setWebDavScheme(newValue)
        }

    var password: String
        get() {
            return defaults.getWebDavPassword() ?: ""
        }
        set(newValue) {
            defaults.setWebDavPassword(newValue.ifEmpty { null })
        }
}