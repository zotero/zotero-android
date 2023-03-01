package org.zotero.android.screens.allitems.data

data class ItemsSortType(
    val field: Field,
    val ascending: Boolean,
) {
    enum class Field {
        creator, date, dateAdded, dateModified, itemType, publicationTitle, publisher, title, year;

        val id: Int get(){
            return ordinal
        }

        val titleStr: String get() {
            when (this) {
                creator -> {
                    return "Creator"
                }
                date -> {
                    return "Date"
                }
                dateAdded-> {
                    return "Date Added"
                }
                dateModified-> {
                    return "Date Modified"
                }
                itemType-> {
                    return "Item Type"
                }
                publicationTitle-> {
                    return "Publication Title"
                }
                publisher-> {
                    return "Publisher"
                }
                title-> {
                    return "Title"
                }
                year-> {
                    return "Year"
                }
            }
        }

        val defaultOrderAscending: Boolean get()  {
            when (this) {
                creator -> {
                    return true
                }
                date -> {
                    return false
                }
                dateAdded -> {
                    return false
                }
                dateModified -> {
                    return false
                }
                itemType -> {
                    return true
                }
                publicationTitle -> {
                    return true
                }
                publisher -> {
                    return true
                }
                title -> {
                    return true
                }
                year -> {
                    return false
                }
            }
        }
    }

    companion object {
        val default: ItemsSortType get() {
            return ItemsSortType(field = Field.title, ascending = true)
        }
    }
}