package org.zotero.android.uicomponents.singlepicker

data class SinglePickerResult(val id: String, val callPoint: CallPoint) {
    enum class CallPoint{
        AllItemsShowItem, AllItemsSortPicker, ItemDetails, CreatorEdit, SettingsWebDav
    }
}
