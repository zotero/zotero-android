package org.zotero.android.architecture.logging.debug

data class PendingCoordinatorAction(
    val ignoreEmptyLogs: Boolean,
    val userId: Long,
    val customAlertMessage: ((String) -> String)? = null

)