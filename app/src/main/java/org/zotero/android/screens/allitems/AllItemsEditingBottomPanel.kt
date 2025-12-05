package org.zotero.android.screens.allitems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AppBarMenuState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import org.zotero.android.database.objects.Attachment
import org.zotero.android.screens.allitems.data.AllItemsBottomPanelItem
import org.zotero.android.screens.allitems.data.AllItemsBottomPanelItems
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun AllItemsEditingBottomPanel(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
) {
    val errorRedColor = MaterialTheme.colorScheme.error

    var panelItems: List<AllItemsBottomPanelItem> = emptyList()
    var overflowItems: List<AllItemsBottomPanelItem> = emptyList()

    val size = viewState.selectedKeys?.size ?: 0
    if (size == 1) {
        val actions = editingSingleItemSelectedActions(
            viewModel = viewModel,
            viewState = viewState,
            errorRedColor = errorRedColor
        )
        panelItems = actions.panelItems
        overflowItems = actions.overflowItems
    } else if (size > 1) {
        val actions = editingMultipleItemsSelectedActions(
            viewModel = viewModel,
            viewState = viewState,
            errorRedColor = errorRedColor
        )
        panelItems = actions.panelItems
        overflowItems = actions.overflowItems
    }

    val maxItemsInPanel = 6
    val paneItemsToDisplay = panelItems.take(maxItemsInPanel)
    val extraOverflowItems =
        if (panelItems.size > maxItemsInPanel) {
            panelItems.subList(maxItemsInPanel, panelItems.size)
        } else{
            emptyList()
        }

    overflowItems = extraOverflowItems + overflowItems

    val menuState = remember { AppBarMenuState() }
    FlexibleBottomAppBar(
        horizontalArrangement = Arrangement.SpaceBetween,
        content = {
            paneItemsToDisplay.forEach {
                AllItemsBottomPanelAppbarContent(
                    overflowTextResId = it.overflowTextResId,
                    onClick = it.onClick,
                    iconRes = it.iconRes,
                    iconTint = it.iconTint
                )
            }
            if (overflowItems.isNotEmpty()) {
                Box {
                    IconButton(
                        onClick = {
                            if (menuState.isExpanded) {
                                menuState.dismiss()
                            } else {
                                menuState.show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Overflow")
                    }

                    DropdownMenu(
                        expanded = menuState.isExpanded,
                        onDismissRequest = { menuState.dismiss() },
                    ) {
                        overflowItems
                            .forEach { item ->
                                AllItemsBottomPanelMenuContent(
                                    onClick = item.onClick,
                                    iconRes = item.iconRes,
                                    iconTint = item.iconTint,
                                    overflowTextResId = item.overflowTextResId,
                                    textColor = item.textColor
                                )
                            }
                    }

                }

            }

        })
}

private fun editingMultipleItemsSelectedActions(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
    errorRedColor: Color
): AllItemsBottomPanelItems {
    val panelItems = mutableListOf<AllItemsBottomPanelItem>()
    val overflowItems = mutableListOf<AllItemsBottomPanelItem>()

    if (viewState.isCollectionTrash) {
        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.restore_trash,
                overflowTextResId = Strings.restore,
                onClick = { viewModel.onTrashRestore() })
        )

        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.delete_24px,
                overflowTextResId = Strings.delete,
                iconTint = errorRedColor,
                textColor = errorRedColor,
                onClick = { viewModel.onTrashDelete() })
        )

        downloadAndRemoveAttachmentBlock(
            viewModel = viewModel,
            panelItems = panelItems,
            overflowItems = overflowItems
        )
    } else {
        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.create_new_folder_24px,
                overflowTextResId = Strings.items_action_add_to_collection,
                onClick = { viewModel.onAddToCollection() })
        )

        if (viewState.isCollectionACollection) {
            overflowItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.remove_from_collection,
                    overflowTextResId = Strings.items_action_remove_from_collection,
                    onClick = { viewModel.onRemoveFromCollection() })
            )
        }


        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.delete_24px,
                overflowTextResId = Strings.move_to_trash,
                iconTint = errorRedColor,
                textColor = errorRedColor,
                onClick = { viewModel.onMoveToTrash() })
        )

        if (viewModel.shouldIncludeCopyCitationAndBibliographyButtons()) {
            panelItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.cite,
                    overflowTextResId = Strings.citation_copy_citation,
                    onClick = { viewModel.onCopyCitation() })
            )

            panelItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.bibliography,
                    overflowTextResId = Strings.citation_copy_bibliography,
                    onClick = { viewModel.onCopyBibliography() })
            )
        }

        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.share,
                overflowTextResId = Strings.share,
                onClick = { viewModel.onShare() }
            ))

        downloadAndRemoveAttachmentBlock(
            viewModel = viewModel,
            panelItems = panelItems,
            overflowItems = overflowItems
        )
    }
    return AllItemsBottomPanelItems(panelItems = panelItems, overflowItems = overflowItems)

}

private fun downloadAndRemoveAttachmentBlock(
    viewModel: AllItemsViewModel,
    panelItems: MutableList<AllItemsBottomPanelItem>,
    overflowItems: MutableList<AllItemsBottomPanelItem>
) {
    panelItems.add(
        AllItemsBottomPanelItem(
            iconRes = Drawables.download_24px,
            overflowTextResId = Strings.items_action_download,
            onClick = { viewModel.onDownloadSelectedAttachments() })
    )

    overflowItems.add(
        AllItemsBottomPanelItem(
            iconRes = Drawables.file_download_off_24px,
            overflowTextResId = Strings.items_action_remove_download,
            onClick = { viewModel.onRemoveSelectedAttachments() })
    )
}


private fun editingSingleItemSelectedActions(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
    errorRedColor: Color,
): AllItemsBottomPanelItems {
    val panelItems = mutableListOf<AllItemsBottomPanelItem>()
    val overflowItems = mutableListOf<AllItemsBottomPanelItem>()

    if (viewState.isCollectionTrash) {
        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.restore_trash,
                overflowTextResId = Strings.restore,
                onClick = { viewModel.onTrashRestore() })
        )

        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.delete_24px,
                overflowTextResId = Strings.delete,
                iconTint = errorRedColor,
                textColor = errorRedColor,
                onClick = { viewModel.onTrashDelete() })
        )
    } else {
        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.create_new_folder_24px,
                overflowTextResId = Strings.items_action_add_to_collection,
                onClick = { viewModel.onAddToCollection() })
        )

        if (viewModel.shouldIncludeRemoveFromCollectionButton()) {
            overflowItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.remove_from_collection,
                    overflowTextResId = Strings.items_action_remove_from_collection,
                    onClick = { viewModel.onRemoveFromCollection() })
            )
        }

        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.delete_24px,
                overflowTextResId = Strings.move_to_trash,
                iconTint = errorRedColor,
                textColor = errorRedColor,
                onClick = { viewModel.onMoveToTrash() })
        )

        if (viewModel.shouldIncludeCopyCitationAndBibliographyButtons()) {
            panelItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.cite,
                    overflowTextResId = Strings.citation_copy_citation,
                    onClick = { viewModel.onCopyCitation() })
            )

            panelItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.bibliography,
                    overflowTextResId = Strings.citation_copy_bibliography,
                    onClick = { viewModel.onCopyBibliography() })
            )
        }

        panelItems.add(
            AllItemsBottomPanelItem(
                iconRes = Drawables.share,
                overflowTextResId = Strings.share,
                onClick = { viewModel.onShare() })
        )

        if (viewModel.shouldIncludeRetrieveMetadataButton()) {
            panelItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.retrieve_metadata_24px,
                    overflowTextResId = Strings.items_action_retrieve_metadata,
                    onClick = { viewModel.onShowRetrieveDialog() })
            )
        }

        if (viewModel.shouldIncludeCreateParentButton()) {
            panelItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.add_24px,
                    overflowTextResId = Strings.items_action_create_parent,
                    onClick = { viewModel.onCreateDialog() })
            )
        }
        val attachmentFileLocation = viewModel.getAttachmentFileLocation()
        if (attachmentFileLocation != null) {
            when (attachmentFileLocation) {
                Attachment.FileLocation.local -> {
                    overflowItems.add(
                        AllItemsBottomPanelItem(
                            iconRes = Drawables.file_download_off_24px,
                            overflowTextResId = Strings.items_action_remove_download,
                            onClick = { viewModel.onRemoveDownload() })
                    )

                }

                Attachment.FileLocation.remote -> {
                    panelItems.add(
                        AllItemsBottomPanelItem(
                            iconRes = Drawables.download_24px,
                            overflowTextResId = Strings.items_action_download,
                            onClick = { viewModel.onDownload() })
                    )
                }

                Attachment.FileLocation.localAndChangedRemotely -> {
                    panelItems.add(
                        AllItemsBottomPanelItem(
                            iconRes = Drawables.download_24px,
                            overflowTextResId = Strings.items_action_download,
                            onClick = { viewModel.onDownload() })
                    )

                    overflowItems.add(
                        AllItemsBottomPanelItem(
                            iconRes = Drawables.file_download_off_24px,
                            overflowTextResId = Strings.items_action_remove_download,
                            onClick = { viewModel.onRemoveDownload() })
                    )
                }

                Attachment.FileLocation.remoteMissing -> {
                    //no-op
                }
            }
        }

        if (viewModel.shouldIncludeDuplicateButton()) {
            overflowItems.add(
                AllItemsBottomPanelItem(
                    iconRes = Drawables.content_copy_24px,
                    overflowTextResId = Strings.items_action_duplicate,
                    onClick = { viewModel.onDuplicate() })
            )
        }

    }
    return AllItemsBottomPanelItems(panelItems = panelItems, overflowItems = overflowItems)

}
