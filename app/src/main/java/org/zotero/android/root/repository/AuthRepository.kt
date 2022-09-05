package org.zotero.android.root.repository

import org.zotero.android.architecture.SdkPrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val sdkPrefs: SdkPrefs
) {
    fun isUserLoggedIn(): Boolean {
        return sdkPrefs.isUserLoggedIn()
    }
}
