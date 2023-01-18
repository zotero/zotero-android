@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.itemdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.dashboard.data.ItemDetailCreator
import org.zotero.android.dashboard.data.ItemDetailField
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CloseIconTopBar
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun ItemDetailsScreen(
    viewModel: ItemDetailsViewModel = hiltViewModel(),
    navigateToCreatorEdit: () -> Unit,
    onBack: () -> Unit,

    ) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(ItemDetailsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()

    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            ItemDetailsViewEffect.ShowCreatorEditEffect -> {
                navigateToCreatorEdit()
            }
            ItemDetailsViewEffect.ScreenRefersh -> {
                //no-op
            }
        }
    }
    CustomScaffold(
        topBar = {
            TopBar(
                onCloseClicked = onBack,
                onViewOrEditClicked = viewModel::onSaveOrEditClicked,
                isEditing = viewState.isEditing
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
            ) {
                if (viewState.isEditing) {
                    ItemDetailsEditScreen(
                        viewState = viewState,
                        layoutType = layoutType,
                        viewModel = viewModel
                    )
                } else {
                    ItemDetailsViewScreen(
                        viewState = viewState,
                        layoutType = layoutType,
                        viewModel = viewModel
                    )
                }

            }
        }

    }

}

@Composable
private fun TopBar(
    onCloseClicked: () -> Unit,
    onViewOrEditClicked: () -> Unit,
    isEditing: Boolean,
) {
    CloseIconTopBar(
        title = stringResource(id = Strings.item_details),
        onClose = onCloseClicked,
        actions = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onViewOrEditClicked,
                text = if (isEditing) stringResource(Strings.save) else stringResource(Strings.edit)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}

private sealed class CellType {
    data class field(val field: ItemDetailField) : CellType()
    data class creator(val creator: ItemDetailCreator) : CellType()
    data class value(val value: String, val title: String) : CellType()
}
