package org.zotero.android.api.pojo.sync

data class CreatorResponse(
    val creatorType: String,
    val firstName: String?,
    val lastName: String?,
    val name: String?,
) {
    val summaryName: String? get() {
        val name = this.name
        if (name != null) {
            return name
        }
        val lastName = this.lastName
        if (lastName != null) {
            return lastName
        }
        return this.firstName
    }
}