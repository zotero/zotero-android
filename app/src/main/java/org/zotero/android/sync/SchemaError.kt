package org.zotero.android.sync

sealed class SchemaError: Exception() {
    data class missingSchemaFields(val itemType: String): SchemaError()
    data class unknownField(val key: String, val field: String):  SchemaError()
    data class missingField(val key: String, val field: String, val itemType: String):  SchemaError()
    data class invalidValue(val value: String, val field: String, val key: String):  SchemaError()
    data class embeddedImageMissingParent(val key: String, val libraryId: LibraryIdentifier):  SchemaError()
}
