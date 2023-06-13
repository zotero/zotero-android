package org.zotero.android.api.pojo.sync

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class GroupResponse(

    @SerializedName("id")
    @Expose
    val identifier: Int,

    @SerializedName("version")
    @Expose
    val version: Int,

    @SerializedName("data")
    @Expose
    val data: Data,
) {
    data class Data(
        @SerializedName("name")
        @Expose
        val name: String,

        @SerializedName("owner")
        @Expose
        val owner: Long,

        @SerializedName("type")
        @Expose
        val type: String,

        @SerializedName("description")
        @Expose
        val description: String,

        @SerializedName("libraryEditing")
        @Expose
        val libraryEditing: String,

        @SerializedName("libraryReading")
        @Expose
        val libraryReading: String,

        @SerializedName("fileEditing")
        @Expose
        val fileEditing: String,

        @SerializedName("admins")
        @Expose
        val admins: List<Long>?,

        @SerializedName("members")
        @Expose
        val members: List<Long>?,
    )
}
