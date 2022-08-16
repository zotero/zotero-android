package org.zotero.android.architecture.app

/**
 * Interface for providing app version for our BE.
 */
interface AppVersionProvider {
    fun provide(): String
}
