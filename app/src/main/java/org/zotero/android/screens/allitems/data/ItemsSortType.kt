package org.zotero.android.screens.allitems.data

import io.realm.Sort

data class ItemsSortType(
    val field: Field,
    val ascending: Boolean,
) {
    enum class Field {
        creator, date, dateAdded, dateModified, itemType, publicationTitle, publisher, title, year;

        val id: Int
            get() {
                return ordinal
            }

        val titleStr: String
            get() {
                when (this) {
                    creator -> {
                        return "Creator"
                    }
                    date -> {
                        return "Date"
                    }
                    dateAdded -> {
                        return "Date Added"
                    }
                    dateModified -> {
                        return "Date Modified"
                    }
                    itemType -> {
                        return "Item Type"
                    }
                    publicationTitle -> {
                        return "Publication Title"
                    }
                    publisher -> {
                        return "Publisher"
                    }
                    title -> {
                        return "Title"
                    }
                    year -> {
                        return "Year"
                    }
                }
            }

        val defaultOrderAscending: Boolean
            get() {
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

    private fun getCurrentSort(): Sort {
        return if (this.ascending) Sort.ASCENDING else Sort.DESCENDING
    }

    val descriptors: Pair<Array<String>, Array<Sort>>
        get() {
            when (this.field) {
                Field.title -> {
                    return arrayOf("sortTitle") to arrayOf(getCurrentSort())
                }
                Field.creator -> {
                    return arrayOf(
                        "hasCreatorSummary",
                        "sortCreatorSummary",
                        "sortTitle"
                    ) to arrayOf(Sort.DESCENDING, getCurrentSort(), Sort.ASCENDING)
                }
                Field.date -> {
                    return arrayOf(
                        "hasParsedDate",
                        "parsedDate",
                        "sortTitle"
                    ) to arrayOf(Sort.DESCENDING, getCurrentSort(), Sort.ASCENDING)
                }
                Field.dateAdded -> {
                    return arrayOf("dateAdded", "sortTitle") to arrayOf(
                        getCurrentSort(),
                        Sort.ASCENDING
                    )
                }
                Field.dateModified -> {
                    return arrayOf("dateModified", "sortTitle") to arrayOf(
                        getCurrentSort(),
                        Sort.ASCENDING
                    )
                }
                Field.itemType -> {
                    return arrayOf("localizedType", "sortTitle") to arrayOf(
                        getCurrentSort(),
                        Sort.ASCENDING
                    )
                }
                Field.publicationTitle -> {
                    return arrayOf(
                        "hasPublicationTitle",
                        "publicationTitle",
                        "sortTitle"
                    ) to arrayOf(Sort.DESCENDING, getCurrentSort(), Sort.ASCENDING)
                }
                Field.publisher -> {
                    return arrayOf(
                        "hasPublisher",
                        "publisher",
                        "sortTitle"
                    ) to arrayOf(Sort.DESCENDING, getCurrentSort(), Sort.ASCENDING)
                }
                Field.year -> {
                    return arrayOf(
                        "hasPublisher",
                        "parsedYear",
                        "sortTitle"
                    ) to arrayOf(Sort.DESCENDING, getCurrentSort(), Sort.ASCENDING)
                }
            }
        }


    companion object {
        val default: ItemsSortType
            get() {
                return ItemsSortType(field = Field.title, ascending = true)
            }
    }
}