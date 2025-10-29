package org.zotero.android.screens.share.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.share.rows.ShareCollectionOptionRow
import org.zotero.android.screens.share.data.CollectionPickerState
import org.zotero.android.screens.share.data.RecentData
import org.zotero.android.screens.share.rows.ShareCollectionErrorRow
import org.zotero.android.screens.share.rows.ShareCollectionMoreRow
import org.zotero.android.screens.share.rows.ShareCollectionProgressRow
import org.zotero.android.sync.Library
import org.zotero.android.uicomponents.Strings
import org.zotero.android.sync.Collection

@Composable
internal fun ShareCollectionsSection(
    navigateToMoreCollections: () -> Unit,
    onCollectionClicked: (collection: Collection?, library: Library) -> Unit,
    collectionPickerState: CollectionPickerState,
    recents: List<RecentData>,
) {
    ShareSectionTitle(
        titleId = Strings.shareext_collection_title
    )
    when (collectionPickerState) {
        CollectionPickerState.failed -> {
            ShareCollectionErrorRow(title = stringResource(id = Strings.shareext_sync_error))
        }

        CollectionPickerState.loading -> {
            ShareCollectionProgressRow()
        }

        is CollectionPickerState.picked -> {
            val library = collectionPickerState.library
            val collection = collectionPickerState.collection
            recents.forEach { recent ->
                val selected =
                    recent.collection?.identifier == collection?.identifier && recent.library.identifier == library.identifier
                val title = recent.collection?.name ?: recent.library.name
                ShareCollectionOptionRow(
                    text = title,
                    isSelected = selected,
                    onOptionSelected = {
                        onCollectionClicked(
                            recent.collection,
                            recent.library
                        )
                    }

                )
            }
            ShareCollectionMoreRow(
                onItemTapped = navigateToMoreCollections,
            )
        }
    }
}
