package org.zotero.android.screens.allitems

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.AppBarRowScope
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.zotero.android.database.objects.Attachment
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun AllItemsEditingBottomPanel(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
) {
    val errorRedColor = MaterialTheme.colorScheme.error

    FlexibleBottomAppBar(
        horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
        contentPadding = PaddingValues(horizontal = 0.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        content = {
            AppBarRow(
                overflowIndicator = { menuState ->
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
                }
            ) {
                val size = viewState.selectedKeys?.size ?: 0
                if (size == 1) {
                    editingSingleItemSelectedActions(
                        viewModel = viewModel,
                        viewState = viewState,
                        errorRedColor = errorRedColor
                    )
                } else if (size > 1) {
                    editingMultipleItemsSelectedActions(
                        viewModel = viewModel,
                        viewState = viewState,
                        errorRedColor = errorRedColor
                    )
                }
            }
        })
}

private fun AppBarRowScope.editingMultipleItemsSelectedActions(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
    errorRedColor: Color
) {
    if (viewState.isCollectionTrash) {
        allItemsBottomPanelItem(
            iconRes = Drawables.restore_trash,
            overflowTextResId = Strings.restore,
            onClick = { viewModel.onTrashRestore() })

        allItemsBottomPanelItem(
            iconRes = Drawables.delete_24px,
            overflowTextResId = Strings.delete,
            iconTint = errorRedColor,
            textColor = errorRedColor,
            onClick = { viewModel.onTrashDelete() })

        DownloadAndRemoveAttachmentBlock(
            viewModel = viewModel,
        )
    } else {
        allItemsBottomPanelItem(
            iconRes = Drawables.create_new_folder_24px,
            overflowTextResId = Strings.items_action_add_to_collection,
            onClick = { viewModel.onAddToCollection() })

        if (viewState.isCollectionACollection) {
            allItemsBottomPanelItem(
                iconRes = Drawables.remove_from_collection,
                overflowTextResId = Strings.items_action_remove_from_collection,
                onClick = { viewModel.onRemoveFromCollection() }
            )
        }


        allItemsBottomPanelItem(
            iconRes = Drawables.delete_24px,
            overflowTextResId = Strings.move_to_trash,
            iconTint = errorRedColor,
            textColor = errorRedColor,
            onClick = { viewModel.onMoveToTrash() })

        if (viewModel.shouldIncludeCopyCitationAndBibliographyButtons()) {
            allItemsBottomPanelItem(
                iconRes = Drawables.cite,
                overflowTextResId = Strings.citation_copy_citation,
                onClick = { viewModel.onCopyCitation() })

            allItemsBottomPanelItem(
                iconRes = Drawables.bibliography,
                overflowTextResId = Strings.citation_copy_bibliography,
                onClick = { viewModel.onCopyBibliography() })
        }

        allItemsBottomPanelItem(
            iconRes = Drawables.share,
            overflowTextResId = Strings.share,
            onClick = { viewModel.onShare() }
        )


        DownloadAndRemoveAttachmentBlock(
            viewModel = viewModel,
        )
    }


}

private fun AppBarRowScope.DownloadAndRemoveAttachmentBlock(
    viewModel: AllItemsViewModel,
) {
    allItemsBottomPanelItem(
        iconRes = Drawables.download_24px,
        overflowTextResId = Strings.items_action_download,
        onClick = { viewModel.onDownloadSelectedAttachments() })

    allItemsBottomPanelItem(
        iconRes = Drawables.file_download_off_24px,
        overflowTextResId = Strings.items_action_remove_download,
        onClick = { viewModel.onRemoveSelectedAttachments() })
}


private fun AppBarRowScope.editingSingleItemSelectedActions(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
    errorRedColor: Color,
) {
    if (viewState.isCollectionTrash) {
        allItemsBottomPanelItem(
            iconRes = Drawables.restore_trash,
            overflowTextResId = Strings.restore,
            onClick = { viewModel.onTrashRestore() })

        allItemsBottomPanelItem(
            iconRes = Drawables.delete_24px,
            overflowTextResId = Strings.delete,
            iconTint = errorRedColor,
            textColor = errorRedColor,
            onClick = { viewModel.onTrashDelete() })
    } else {
        allItemsBottomPanelItem(
            iconRes = Drawables.create_new_folder_24px,
            overflowTextResId = Strings.items_action_add_to_collection,
            onClick = { viewModel.onAddToCollection() })

        if (viewModel.shouldIncludeRemoveFromCollectionButton()) {
            allItemsBottomPanelItem(
                iconRes = Drawables.remove_from_collection,
                overflowTextResId = Strings.items_action_remove_from_collection,
                onClick = { viewModel.onRemoveFromCollection() })
        }

        allItemsBottomPanelItem(
            iconRes = Drawables.delete_24px,
            overflowTextResId = Strings.move_to_trash,
            iconTint = errorRedColor,
            textColor = errorRedColor,
            onClick = { viewModel.onMoveToTrash() })

        if (viewModel.shouldIncludeCopyCitationAndBibliographyButtons()) {
            allItemsBottomPanelItem(
                iconRes = Drawables.cite,
                overflowTextResId = Strings.citation_copy_citation,
                onClick = { viewModel.onCopyCitation() })

            allItemsBottomPanelItem(
                iconRes = Drawables.bibliography,
                overflowTextResId = Strings.citation_copy_bibliography,
                onClick = { viewModel.onCopyBibliography() })
        }

        allItemsBottomPanelItem(
            iconRes = Drawables.share,
            overflowTextResId = Strings.share,
            onClick = { viewModel.onShare() }
        )

        if (viewModel.shouldIncludeRetrieveMetadataButton()) {
            allItemsBottomPanelItem(
                iconRes = Drawables.retrieve_metadata_24px,
                overflowTextResId = Strings.items_action_retrieve_metadata,
                onClick = { viewModel.onShowRetrieveDialog() })
        }

        if (viewModel.shouldIncludeCreateParentButton()) {
            allItemsBottomPanelItem(
                iconRes = Drawables.add_24px,
                overflowTextResId = Strings.items_action_create_parent,
                onClick = { viewModel.onCreateDialog() })
        }
        val attachmentFileLocation = viewModel.getAttachmentFileLocation()
        if (attachmentFileLocation != null) {
            when (attachmentFileLocation) {
                Attachment.FileLocation.local -> {
                    allItemsBottomPanelItem(
                        iconRes = Drawables.file_download_off_24px,
                        overflowTextResId = Strings.items_action_remove_download,
                        onClick = { viewModel.onRemoveDownload() })

                }

                Attachment.FileLocation.remote -> {
                    allItemsBottomPanelItem(
                        iconRes = Drawables.download_24px,
                        overflowTextResId = Strings.items_action_download,
                        onClick = { viewModel.onDownload() })
                }

                Attachment.FileLocation.localAndChangedRemotely -> {
                    allItemsBottomPanelItem(
                        iconRes = Drawables.download_24px,
                        overflowTextResId = Strings.items_action_download,
                        onClick = { viewModel.onDownload() })

                    allItemsBottomPanelItem(
                        iconRes = Drawables.file_download_off_24px,
                        overflowTextResId = Strings.items_action_remove_download,
                        onClick = { viewModel.onRemoveDownload() })
                }

                Attachment.FileLocation.remoteMissing -> {
                    //no-op
                }
            }
        }

        if (viewModel.shouldIncludeDuplicateButton()) {
            allItemsBottomPanelItem(
                iconRes = Drawables.content_copy_24px,
                overflowTextResId = Strings.items_action_duplicate,
                onClick = { viewModel.onDuplicate() })
        }

    }

}
