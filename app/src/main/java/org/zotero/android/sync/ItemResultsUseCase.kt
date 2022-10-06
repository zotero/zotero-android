package org.zotero.android.sync

import androidx.lifecycle.MutableLiveData
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.architecture.database.objects.ItemTypes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemResultsUseCase @Inject constructor() {

    val resultLiveData: MutableLiveData<CustomResult<List<ItemResponse>>> = MutableLiveData(
        CustomResult.GeneralSuccess(emptyList())
    )

    fun postResults(items: List<ItemResponse>) {
        val oldList = getExistingList()
        var newList = oldList + items

        newList = findChildrenForParents(newList)

        resultLiveData.value =
            CustomResult.GeneralSuccess(newList.filter { it.rawType != ItemTypes.annotation })
    }

    private fun findChildrenForParents(itemsList: List<ItemResponse>): List<ItemResponse> {
        val updatedList = mutableListOf<ItemResponse>()

        for (i in itemsList) {
            if (i.rawType == ItemTypes.note) {
                val noteParentKey = i.parentKey
                for (k in itemsList) {
                    if (k.key == noteParentKey) {
                        k.notes.add(i.note ?: "No Note")
                    }
                }
            } else if (i.rawType == ItemTypes.attachment && i.parentKey != null) {
                val attachmentParentKey = i.parentKey
                for (k in itemsList) {
                    if (k.key == attachmentParentKey) {
                        k.attachments.add(i.title ?: "No Title")
                    }
                }
            } else {
                updatedList.add(i)
            }
        }
        return updatedList
    }

    fun postError(e: Throwable) {
        resultLiveData.value = CustomResult.GeneralError.CodeError(e)
    }

    fun getItemById(itemId: String): ItemResponse? {
        val list = getExistingList()
        return list.find { it.key == itemId }
    }

    private fun getExistingList(): List<ItemResponse> {
        return (resultLiveData.value as CustomResult.GeneralSuccess).value
    }
}