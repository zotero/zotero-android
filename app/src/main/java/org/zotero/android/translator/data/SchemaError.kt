package org.zotero.android.translator.data

sealed class SchemaError : Exception() {

    object cantFindFile: SchemaError()
    object incompatibleItem: SchemaError()
    object javascriptCallMissingResult: SchemaError()
    object noSuccessfulTranslators: SchemaError()
    object webExtractionMissingJs: SchemaError()
    object webExtractionMissingData: SchemaError()
}
