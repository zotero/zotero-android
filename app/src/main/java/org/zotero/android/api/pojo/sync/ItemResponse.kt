package org.zotero.android.api.pojo.sync

import com.google.gson.JsonObject
import java.util.Date

data class ItemResponse(
    val rawType: String,
    val key: String,
    val library: LibraryResponse,
    val parentKey: String?,
    val collectionKeys: Set<String>,
    val links: LinksResponse?,
    val parsedDate: String?,
    val isTrash: Boolean,
    val version: Int,
    val dateModified: Date,
    val dateAdded: Date,
    val tags: List<TagResponse>,
    val creators: List<CreatorResponse>,
    val relations: JsonObject,
    val inPublications: Boolean,
    val createdBy: UserResponse?,
    val lastModifiedBy: UserResponse?,
    )