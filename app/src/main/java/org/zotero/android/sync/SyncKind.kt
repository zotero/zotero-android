package org.zotero.android.sync;

enum class SyncKind {
    normal,
    ignoreIndividualDelays,
    full,
    collectionsOnly,
    keysOnly,
    prioritizeDownloads
}