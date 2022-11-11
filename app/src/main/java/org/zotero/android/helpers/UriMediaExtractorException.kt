package org.zotero.android.helpers

class UriMediaExtractorException : Exception {
    constructor(message: String?) : super(message) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}