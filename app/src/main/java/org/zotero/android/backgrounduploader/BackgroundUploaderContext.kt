package org.zotero.android.backgrounduploader

import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import javax.inject.Inject

class BackgroundUploaderContext @Inject constructor(private val defaults: Defaults, private val fileStore: FileStore) {
    private val activeKey = "uploads"
    private val sessionIdsKey = "activeUrlSessionIds"
    private val extensionSessionIdsKey = "shareExtensionObservedUrlSessionIds"

    val sessionIds: List<String> get() {
       return fileStore.getSessionIds() ?: emptyList()
    }

    fun saveSession(identifier: String) {
        var ids = this.sessionIds.toMutableList()
        ids.add(identifier)
        saveSessions(ids)
    }

    fun saveSessions(identifiers: List<String>) {
        fileStore.saveSessions(identifiers)
    }

    fun deleteSession(identifier: String) {
        var ids = this.sessionIds.toMutableList()
        val index = ids.indexOfFirst { it == identifier}
        if (index == -1) {
            return
        }
        ids.removeAt(index)
        saveSessions(ids)
    }

    fun deleteAllSessionIds() {
        fileStore.deleteAllSessionIds()
    }

    val shareExtensionSessionIds: List<String>
        get() {
            return fileStore.getShareExtensionSessionIds() ?: emptyList()
        }

    fun saveShareExtensionSession(identifier: String) {
        var ids = this.shareExtensionSessionIds.toMutableList()
        ids.add(identifier)
        fileStore.saveShareExtensionSessions(ids)
    }

    fun saveShareExtensionSessions(identifiers: List<String>) {
        fileStore.saveShareExtensionSessions(identifiers)
    }

    fun deleteShareExtensionSession(identifier: String) {
        var ids = this.shareExtensionSessionIds.toMutableList()
        val index = ids.indexOfFirst { it == identifier }
        if (index == -1) {
            return
        }
        ids.removeAt(index)
        saveShareExtensionSessions(ids)
    }

    val uploads: List<BackgroundUpload> get() {
        return fileStore.getUploads()?.map { it.value } ?: emptyList()
    }

    val uploadsWithTaskIds: Map<Int, BackgroundUpload> get() {
        return fileStore.getUploads() ?: emptyMap()
    }

    fun loadUpload(taskId: Int): BackgroundUpload? {
        return uploadsWithTaskIds[taskId]
    }

    fun loadUploads(sessionId: String): List<Pair<Int, BackgroundUpload>> {
        val allUploads = uploadsWithTaskIds
        var result = mutableListOf<Pair<Int, BackgroundUpload>>()
        for (entry in allUploads.entries) {
            val taskId = entry.key
            val upload = entry.value
            if (upload.sessionId != sessionId) {
                continue
            }
            result.add(taskId to upload)
        }
        return result
    }

    fun save(upload: BackgroundUpload, taskId: Int) {
        var uploads = uploadsWithTaskIds.toMutableMap()
        uploads[taskId] = upload
        save(uploads)
    }

    fun save(uploads: Map<Int, BackgroundUpload>) {
        fileStore.saveUploads(uploads)
    }

    fun deleteUpload(taskId: Int) {
        var uploads = uploadsWithTaskIds.toMutableMap()
        uploads.remove(taskId)
        save(uploads)
    }

    fun deleteUploads(taskIds: List<Int>) {
        var uploads = uploadsWithTaskIds.toMutableMap()
        for (taskId in taskIds) {
            uploads.remove(taskId)
        }
        save(uploads)
    }

    fun deleteAllUploads() {
        fileStore.deleteAllUploads()
    }
}