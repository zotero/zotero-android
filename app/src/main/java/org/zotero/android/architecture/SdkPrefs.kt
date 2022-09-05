package org.zotero.android.architecture

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SdkPrefs @Inject constructor(
    private val context: Context,
) {
    private val sharedPrefsFile = "ZoteroPrefs"
    private val userId = "userId"
    private val name = "name"
    private val username = "username"
    private val displayName = "displayName"
    private val apiToken = "apiToken"

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(
            sharedPrefsFile,
            Context.MODE_PRIVATE
        )
    }

    fun setUserId(str: Long) {
        sharedPreferences.edit { putLong(userId, str) }
    }

    fun setName(str: String) {
        sharedPreferences.edit { putString(name, str) }
    }

    fun setUsername(str: String) {
        sharedPreferences.edit { putString(username, str) }
    }

    fun setDisplayName(str: String) {
        sharedPreferences.edit { putString(displayName, str) }
    }

    fun setApiToken(str: String) {
        sharedPreferences.edit { putString(apiToken, str) }
    }

    fun getApiToken(): String? {
        return sharedPreferences.getString(apiToken, null )
    }

    fun isUserLoggedIn() :Boolean {
        return getApiToken() != null
    }

    fun getUserId(): Long {
        return sharedPreferences.getLong(userId, 0L)
    }

}
