package org.zotero.android.api

interface ApiConfiguration {
    val appVersion: String
    val domain: String
    val networkTimeout: Long
}