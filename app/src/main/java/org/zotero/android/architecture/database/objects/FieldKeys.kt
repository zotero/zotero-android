package org.zotero.android.architecture.database.objects

import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import timber.log.Timber

class FieldKeys {
    class Collection {
        companion object {
            val knownDataKeys = listOf("key", "version", "name", "parentCollection", "relations")
        }
    }

    class Item {

        companion object {
            val title = "title"
            val abstractN = "abstractNote"
            val note = "note"
            val date = "date"
            val reporter = "reporter"
            val court = "court"
            val publisher = "publisher"
            val publicationTitle = "publicationTitle"
            val doi = "DOI"
            val url = "url"
            val accessDate = "accessDate"
            val extra = "extra"

            fun clean(doi: String): String {
                if (doi.isEmpty()) {
                    return ""
                }

                try {
                    val regex = """10(?:\.[0-9]{4,})?\/[^\s]*[^\s\.,]""".toRegex()
                    val match = regex.find(doi)
                    if (match != null) {
                        return match.value
                    }
                    return ""
                } catch (e: Exception) {
                    Timber.e("org.zotero.android.architecture.database.objects.FieldKeys: can't clean DOI - $e")
                    return ""
                }
            }

            fun isDoi(value: String): Boolean {
                return !clean(doi = value).isEmpty()
            }

            val knownNonFieldKeys = listOf(
                "creators",
                "itemType",
                "version",
                "key",
                "tags",
                "deleted",
                "collections",
                "relations",
                "dateAdded",
                "dateModified",
                "parentItem",
                "inPublications"
            )
        }


        class Attachment {
            companion object {
                val linkMode = "linkMode"
                val contentType = "contentType"
                val md5 = "md5"
                val mtime = "mtime"
                val title = "title"
                val filename = "filename"
                val url = "url"
                val charset = "charset"
                val path = "path"

                val knownKeys: Set<String>
                    get() {
                        return LinkedHashSet(
                            listOf(
                                title,
                                contentType,
                                md5,
                                mtime,
                                filename,
                                linkMode,
                                charset,
                                path,
                                url
                            )
                        )
                    }

                val fieldKeys: List<String>
                    get() {
                        return listOf(
                            Item.title,
                            filename,
                            contentType,
                            linkMode,
                            md5,
                            mtime,
                            url
                        )
                    }
            }
        }

        class Annotation {
            class Position {
                companion object {
                    val pageIndex = "pageIndex"
                    val rects = "rects"
                    val paths = "paths"
                    val lineWidth = "width"
                }

            }


            companion object {
                val type = "annotationType"
                val text = "annotationText"
                val comment = "annotationComment"
                val color = "annotationColor"
                val pageLabel = "annotationPageLabel"
                val sortIndex = "annotationSortIndex"
                val position = "annotationPosition"
                val authorName = "annotationAuthorName"

                val knownKeys: Set<String>
                    get() {
                        return LinkedHashSet(
                            listOf(
                                color,
                                comment,
                                pageLabel,
                                position,
                                text,
                                type,
                                sortIndex,
                                authorName
                            )
                        )
                    }

                fun fields(annotationType: AnnotationType): List<KeyBaseKeyPair> {
                    when (annotationType) {
                        AnnotationType.highlight -> {
                            return listOf(
                                KeyBaseKeyPair(key = type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = pageLabel, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                                KeyBaseKeyPair(key = text, baseKey = null),
                                KeyBaseKeyPair(
                                    key = Position.pageIndex,
                                    baseKey = position
                                )
                            )
                        }

                        AnnotationType.ink -> {
                            return listOf(
                                KeyBaseKeyPair(key = type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = pageLabel, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                                KeyBaseKeyPair(
                                    key = Position.pageIndex,
                                    baseKey = position
                                ),
                                KeyBaseKeyPair(
                                    key = Position.lineWidth,
                                    baseKey = position
                                )
                            )
                        }

                        AnnotationType.note, AnnotationType.image -> {
                            return listOf(
                                KeyBaseKeyPair(key = type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = pageLabel, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                                KeyBaseKeyPair(
                                    key = Position.pageIndex,
                                    baseKey = position
                                )
                            )
                        }

                    }
                }

            }
        }
    }

    class Search {
        companion object {
            val knownDataKeys = listOf("key", "version", "name", "conditions")
        }

    }
}
