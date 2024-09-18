package org.zotero.android.architecture

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.files.DataMarshaller
import org.zotero.android.pdf.data.PDFSettings
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.webdav.data.WebDavScheme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class Defaults @Inject constructor(
    private val context: Context,
    private val dataMarshaller: DataMarshaller,
) {
    private val sharedPrefsFile = "ZoteroPrefs"
    private val userId = "userId"
    private val name = "name"
    private val username = "username"
    private val displayName = "displayName"
    private val apiToken = "apiToken"
    private val showSubcollectionItems = "showSubcollectionItems"
    private val lastUsedCreatorNamePresentation = "LastUsedCreatorNamePresentation"
    private val itemsSortType = "ItemsSortType"
    private val showCollectionItemCounts = "showCollectionItemCounts"
    private val performFullSyncGuardKey = "performFullSyncGuardKey"
    private val didPerformFullSyncFix = "didPerformFullSyncFix"
    private val tagPickerShowAutomaticTags = "tagPickerShowAutomaticTags"
    private val tagPickerDisplayAllTags = "tagPickerDisplayAllTags"
    private val isDebugLogEnabled = "isDebugLogEnabled"
    private val wasPspdfkitInitialized = "wasPspdfkitInitialized"
    private val pdfSettings = "pdfSettings"
    private val highlightColorHex = "highlightColorHex"
    private val noteColorHex = "noteColorHex"
    private val squareColorHex = "squareColorHex"
    private val inkColorHex = "inkColorHex"
    private val underlineColorHex = "underlineColorHex"
    private val textColorHex = "textColorHex"
    private val activeLineWidth = "activeLineWidth"
    private val activeEraserSize = "activeEraserSize"
    private val activeFontSize = "activeFontSize"
    private val shareExtensionIncludeTags = "shareExtensionIncludeTags"

    private val lastTimestamp = "lastTimestamp"
    private val lastTranslationCommitHash = "lastTranslationCommitHash"
    private val lastTranslatorCommitHash = "lastTranslatorCommitHash"
    private val lastTranslatorDeleted = "lastTranslatorDeleted"
    private val lastStylesCommitHash = "lastStylesCommitHash"

    private val isWebDavEnabled = "isWebDavEnabled"
    private val webDavVerified = "webDavVerified"
    private val webDavUsername = "webDavUsername"
    private val webDavUrl = "webDavUrl"
    private val webDavScheme = "webDavScheme"
    private val webDavPassword = "webDavPassword"

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(
            sharedPrefsFile,
            Context.MODE_PRIVATE
        )
    }

    fun setHighlightColorHex(str: String) {
        sharedPreferences.edit { putString(highlightColorHex, str) }
    }

    fun getHighlightColorHex(): String {
        return sharedPreferences.getString(highlightColorHex, AnnotationsConfig.defaultActiveColor )!!
    }

    fun setNoteColorHex(str: String) {
        sharedPreferences.edit { putString(noteColorHex, str) }
    }

    fun getNoteColorHex(): String {
        return sharedPreferences.getString(noteColorHex, AnnotationsConfig.defaultActiveColor )!!
    }

    fun setSquareColorHex(str: String) {
        sharedPreferences.edit { putString(squareColorHex, str) }
    }

    fun getSquareColorHex(): String {
        return sharedPreferences.getString(squareColorHex, AnnotationsConfig.defaultActiveColor )!!
    }

    fun setInkColorHex(str: String) {
        sharedPreferences.edit { putString(inkColorHex, str) }
    }

    fun getInkColorHex(): String {
        return sharedPreferences.getString(inkColorHex, AnnotationsConfig.defaultActiveColor )!!
    }

    fun setTextColorHex(str: String) {
        sharedPreferences.edit { putString(textColorHex, str) }
    }

    fun getTextColorHex(): String {
        return sharedPreferences.getString(textColorHex, AnnotationsConfig.defaultActiveColor )!!
    }

    fun setUnderlineColorHex(str: String) {
        sharedPreferences.edit { putString(underlineColorHex, str) }
    }

    fun getUnderlineColorHex(): String {
        return sharedPreferences.getString(underlineColorHex, AnnotationsConfig.defaultActiveColor )!!
    }

    fun setActiveLineWidth(width: Float) {
        sharedPreferences.edit { putFloat(activeLineWidth, width) }
    }

    fun getActiveLineWidth(): Float {
        return sharedPreferences.getFloat(activeLineWidth, 2f )
    }

    fun setActiveEraserSize(width: Float) {
        sharedPreferences.edit { putFloat(activeEraserSize, width) }
    }

    fun getActiveEraserSize(): Float {
        return sharedPreferences.getFloat(activeEraserSize, 10f )
    }

    fun setActiveFontSize(size: Float) {
        sharedPreferences.edit { putFloat(activeFontSize, size) }
    }

    fun getActiveFontSize(): Float {
        return sharedPreferences.getFloat(activeFontSize, 12f )
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

    fun getUsername(): String {
        return sharedPreferences.getString(username, "" )!!
    }

    fun setDisplayName(str: String) {
        sharedPreferences.edit { putString(displayName, str) }
    }

    fun getDisplayName(): String {
        return sharedPreferences.getString(displayName, "" )!!
    }

    fun setApiToken(str: String?) {
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

    fun showSubcollectionItems(): Boolean {
        return sharedPreferences.getBoolean(showSubcollectionItems, false)
    }

    fun setShowSubcollectionItems(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(showSubcollectionItems, newValue) }
    }

    fun setCreatorNamePresentation(namePresentation: ItemDetailCreator.NamePresentation) {
        val json = dataMarshaller.marshal(namePresentation)
        sharedPreferences.edit { putString(lastUsedCreatorNamePresentation, json) }
    }

    fun getCreatorNamePresentation(): ItemDetailCreator.NamePresentation {
        val json: String = sharedPreferences.getString(
            lastUsedCreatorNamePresentation,
            null
        )
            ?: return ItemDetailCreator.NamePresentation.separate
        return dataMarshaller.unmarshal(json)
    }

    fun setItemsSortType(sortType: ItemsSortType) {
        val json = dataMarshaller.marshal(sortType)
        sharedPreferences.edit { putString(itemsSortType, json) }
    }

    fun getItemsSortType(): ItemsSortType {
        val json: String = sharedPreferences.getString(
            itemsSortType,
            null
        )
            ?: return ItemsSortType.default
        return dataMarshaller.unmarshal(json)
    }

    fun getPDFSettings(): PDFSettings {
        val json: String = sharedPreferences.getString(
            this.pdfSettings,
            null
        ) ?: return PDFSettings.default()
        return dataMarshaller.unmarshal(json)
    }

    fun setPDFSettings(
        pdfSettings: PDFSettings,
    ) {
        val json = dataMarshaller.marshal(pdfSettings)
        sharedPreferences.edit { putString(this@Defaults.pdfSettings, json) }
    }

    fun showCollectionItemCounts(): Boolean {
        return sharedPreferences.getBoolean(showCollectionItemCounts, true)
    }

    fun setShowCollectionItemCounts(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(showCollectionItemCounts, newValue) }
    }

    fun didPerformFullSyncFix(): Boolean {
        return sharedPreferences.getBoolean(didPerformFullSyncFix, false)
    }

    fun setDidPerformFullSyncFix(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(didPerformFullSyncFix, newValue) }
    }

    fun isShareExtensionIncludeAttachment(): Boolean {
        return sharedPreferences.getBoolean(shareExtensionIncludeTags, true)
    }

    fun setShareExtensionIncludeAttachment(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(shareExtensionIncludeTags, newValue) }
    }

    fun setTagPickerShowAutomaticTags(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(tagPickerShowAutomaticTags, newValue) }
    }

    fun isTagPickerShowAutomaticTags(): Boolean {
        return sharedPreferences.getBoolean(tagPickerShowAutomaticTags, true)
    }

    fun setTagPickerDisplayAllTags(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(tagPickerDisplayAllTags, newValue) }
    }

    fun isTagPickerDisplayAllTags(): Boolean {
        return sharedPreferences.getBoolean(tagPickerDisplayAllTags, true)
    }

    fun setDebugLogEnabled(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(isDebugLogEnabled, newValue) }
    }

    fun isDebugLogEnabled(): Boolean {
        return sharedPreferences.getBoolean(isDebugLogEnabled, false)
    }

    fun setPspdfkitInitialized(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(wasPspdfkitInitialized, newValue) }
    }

    fun wasPspdfkitInitialized(): Boolean {
        return sharedPreferences.getBoolean(wasPspdfkitInitialized, false)
    }

    fun setLastTimestamp(newValue: Long) {
        sharedPreferences.edit { putLong(lastTimestamp, newValue) }
    }

    fun getLastTimestamp(): Long {
        return sharedPreferences.getLong(lastTimestamp, 0L)
    }

    fun setLastTranslatorCommitHash(newValue: String) {
        sharedPreferences.edit { putString(lastTranslatorCommitHash, newValue) }
    }

    fun getLastTranslatorCommitHash(): String {
        return sharedPreferences.getString(lastTranslatorCommitHash, "") ?: ""
    }

    fun setLastTranslationCommitHash(newValue: String) {
        sharedPreferences.edit { putString(lastTranslationCommitHash, newValue) }
    }

    fun getLastTranslationCommitHash(): String {
        return sharedPreferences.getString(lastTranslationCommitHash, "") ?: ""
    }

    fun getLastTranslatorDeleted(): Long {
        return sharedPreferences.getLong(lastTranslatorDeleted, 0L)
    }

    fun setLastTranslatorDeleted(newValue: Long) {
        sharedPreferences.edit { putLong(lastTranslatorDeleted, newValue) }
    }

    fun setLastStylesCommitHash(newValue: String) {
        sharedPreferences.edit { putString(lastStylesCommitHash, newValue) }
    }

    fun getLastStylesCommitHash(): String {
        return sharedPreferences.getString(lastStylesCommitHash, "") ?: ""
    }

    fun setWebDavEnabled(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(isWebDavEnabled, newValue) }
    }

    fun isWebDavEnabled(): Boolean {
        return sharedPreferences.getBoolean(isWebDavEnabled, false)
    }

    fun setWebDavVerified(newValue: Boolean) {
        sharedPreferences.edit { putBoolean(webDavVerified, newValue) }
    }

    fun isWebDavVerified(): Boolean {
        return sharedPreferences.getBoolean(webDavVerified, false)
    }

    fun setWebDavUsername(str: String?) {
        sharedPreferences.edit { putString(webDavUsername, str) }
    }

    fun getWebDavUsername(): String? {
        return sharedPreferences.getString(webDavUsername, null )
    }

    fun setWebDavPassword(str: String?) {
        sharedPreferences.edit { putString(webDavPassword, str) }
    }

    fun getWebDavPassword(): String? {
        return sharedPreferences.getString(webDavPassword, null )
    }

    fun setWebDavUrl(str: String?) {
        sharedPreferences.edit { putString(webDavUrl, str) }
    }

    fun getWebDavUrl(): String? {
        return sharedPreferences.getString(webDavUrl, null )
    }


    fun setWebDavScheme(scheme: WebDavScheme) {
        val json = dataMarshaller.marshal(scheme)
        sharedPreferences.edit { putString(webDavScheme, json) }
    }

    fun getWebDavScheme(): WebDavScheme {
        val json: String = sharedPreferences.getString(
            webDavScheme,
            null
        )
            ?: return WebDavScheme.https
        return dataMarshaller.unmarshal(json)
    }

    val currentPerformFullSyncGuard = 1

    fun setPerformFullSyncGuard(newValue: Int) {
        sharedPreferences.edit { putInt(performFullSyncGuardKey, newValue) }
    }

    fun performFullSyncGuard(): Int {
        if (!sharedPreferences.contains(performFullSyncGuardKey)) {
            if (sharedPreferences.contains(didPerformFullSyncFix)) {
                return currentPerformFullSyncGuard - 1
            }
            return currentPerformFullSyncGuard
        }
        return sharedPreferences.getInt(performFullSyncGuardKey, 1)
    }

    fun reset() {
        setUsername("")
        setDisplayName("")
        setUserId(0L)
        setShowSubcollectionItems(false)
        setApiToken(null)
        setItemsSortType(ItemsSortType.default)

        setActiveLineWidth(1f)
        setInkColorHex(AnnotationsConfig.defaultActiveColor)
        setSquareColorHex(AnnotationsConfig.defaultActiveColor)
        setNoteColorHex(AnnotationsConfig.defaultActiveColor)
        setHighlightColorHex(AnnotationsConfig.defaultActiveColor)
        setUnderlineColorHex(AnnotationsConfig.defaultActiveColor)
        setTextColorHex(AnnotationsConfig.defaultActiveColor)
        setPDFSettings(pdfSettings = PDFSettings.default())

        setWebDavUrl(null)
        setWebDavScheme(WebDavScheme.https)
        setWebDavEnabled(false)
        setWebDavUsername(null)
        setWebDavPassword(null)
        setWebDavVerified(false)
    }

}
