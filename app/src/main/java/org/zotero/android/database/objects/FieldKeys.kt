package org.zotero.android.database.objects

import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import timber.log.Timber

class FieldKeys {
    class Collection {
        companion object {
            val knownDataKeys =
                listOf("key", "version", "name", "parentCollection", "relations", "deleted")
        }
    }

    class Item {

        companion object {
            const val title = "title"
            const val abstractN = "abstractNote"
            const val note = "note"
            const val date = "date"
            const val reporter = "reporter"
            const val court = "court"
            const val publisher = "publisher"
            const val publicationTitle = "publicationTitle"
            const val doi = "DOI"
            const val url = "url"
            const val accessDate = "accessDate"
            const val extra = "extra"

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
                const val linkMode = "linkMode"
                const val contentType = "contentType"
                const val md5 = "md5"
                const val mtime = "mtime"
                const val title = "title"
                const val filename = "filename"
                const val url = "url"
                const val charset = "charset"
                const val path = "path"

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
                            title,
                            filename,
                            contentType,
                            linkMode,
                            md5,
                            mtime,
                            url,
                            accessDate
                        )
                    }
            }
        }

        class Annotation {
            class Position {
                companion object {
                    const val pageIndex = "pageIndex"
                    const val rects = "rects"
                    const val paths = "paths"
                    const val lineWidth = "width"
                    const val rotation = "rotation"
                    const val fontSize = "fontSize"
                }

            }


            companion object {
                const val type = "annotationType"
                const val text = "annotationText"
                const val comment = "annotationComment"
                const val color = "annotationColor"
                const val pageLabel = "annotationPageLabel"
                const val sortIndex = "annotationSortIndex"
                const val position = "annotationPosition"
                const val authorName = "annotationAuthorName"

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

                fun mandatoryApiFields(annotationType: AnnotationType): List<KeyBaseKeyPair> {
                    when (annotationType) {
                        AnnotationType.highlight, AnnotationType.underline -> {
                            return listOf(
                                KeyBaseKeyPair(key = type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                                KeyBaseKeyPair(key = text, baseKey = null),
                            )
                        }

                        AnnotationType.ink -> {
                            return listOf(
                                KeyBaseKeyPair(key = type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                            )
                        }

                        AnnotationType.note, AnnotationType.image -> {
                            return listOf(
                                KeyBaseKeyPair(key = type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                            )
                        }
                        AnnotationType.text -> {
                            return listOf(
                                KeyBaseKeyPair(key = type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                            )
                        }

                    }
                }

                fun allPDFFields(type: AnnotationType): List<KeyBaseKeyPair> {
                    when(type) {
                        AnnotationType.highlight, AnnotationType.underline -> {
                            return listOf(
                                KeyBaseKeyPair(key = Companion.type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = pageLabel, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                                KeyBaseKeyPair(key = text, baseKey = null),
                                KeyBaseKeyPair(
                                    key = Position.pageIndex,
                                    baseKey = position
                                ),
                            )
                        }

                        AnnotationType.ink -> {
                            return listOf(
                                KeyBaseKeyPair(key = Companion.type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = pageLabel, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                                KeyBaseKeyPair(
                                    key = Annotation.Position.pageIndex,
                                    baseKey = Annotation.position
                                ),
                                KeyBaseKeyPair(
                                    key = Annotation.Position.lineWidth,
                                    baseKey = Annotation.position
                                ),
                            )
                        }

                        AnnotationType.note, AnnotationType.image -> {
                            return listOf(
                                KeyBaseKeyPair(key = Companion.type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = pageLabel, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                                KeyBaseKeyPair(
                                    key = Annotation.Position.pageIndex,
                                    baseKey = Annotation.position
                                ),
                            )
                        }

                        AnnotationType.text -> {
                            return listOf(
                                KeyBaseKeyPair(key = Companion.type, baseKey = null),
                                KeyBaseKeyPair(key = comment, baseKey = null),
                                KeyBaseKeyPair(key = color, baseKey = null),
                                KeyBaseKeyPair(key = pageLabel, baseKey = null),
                                KeyBaseKeyPair(key = sortIndex, baseKey = null),
                                KeyBaseKeyPair(
                                    key = Annotation.Position.pageIndex,
                                    baseKey = Annotation.position
                                ),
                                KeyBaseKeyPair(
                                    key = Annotation.Position.fontSize,
                                    baseKey = Annotation.position
                                ),
                                KeyBaseKeyPair(
                                    key = Annotation.Position.rotation,
                                    baseKey = Annotation.position
                                ),
                            )
                        }
                    }
                }

            }
        }
    }

    class Search {
        companion object {
            val knownDataKeys = listOf("key", "version", "name", "conditions", "deleted")
        }

    }
}
