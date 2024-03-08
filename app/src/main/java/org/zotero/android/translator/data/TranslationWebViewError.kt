package org.zotero.android.translator.data

sealed class TranslationWebViewError : Exception() {

    object cantFindFile: TranslationWebViewError()
    object incompatibleItem: TranslationWebViewError()
    object javascriptCallMissingResult: TranslationWebViewError()
    object noSuccessfulTranslators: TranslationWebViewError()
    object webExtractionMissingJs: TranslationWebViewError()
    object webExtractionMissingData: TranslationWebViewError()
}
