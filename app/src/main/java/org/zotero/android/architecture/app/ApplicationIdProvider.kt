package org.zotero.android.architecture.app

/**
 * Interface for providing app application id throughout the app namely for FileProvider authority.
 */
interface ApplicationIdProvider {
    fun provide(): String
}
