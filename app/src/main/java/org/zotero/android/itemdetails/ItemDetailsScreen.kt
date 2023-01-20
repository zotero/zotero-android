@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.itemdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.dashboard.data.ItemDetailCreator
import org.zotero.android.dashboard.data.ItemDetailField
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
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
            ItemDetailsViewEffect.ScreenRefresh -> {
                //no-op
            }
            ItemDetailsViewEffect.OnBack -> {
                onBack()
            }
        }
    }
    CustomScaffold(
        topBar = {
            TopBar(
                onViewOrEditClicked = viewModel::onSaveOrEditClicked,
                onCancelOrBackClicked = viewModel::onCancelOrBackClicked,
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
    onViewOrEditClicked: () -> Unit,
    onCancelOrBackClicked: () -> Unit,
    isEditing: Boolean,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
) {

    TopAppBar(
        title = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onCancelOrBackClicked,
                text = if (isEditing) stringResource(Strings.cancel) else stringResource(Strings.all_items)
            )
        },
        actions = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onViewOrEditClicked,
                text = if (isEditing) stringResource(Strings.save) else stringResource(Strings.edit)
            )
            Spacer(modifier = Modifier.width(8.dp))
        },
        backgroundColor = CustomTheme.colors.surface,
        elevation = elevation,
    )

}

private sealed class CellType {
    data class field(val field: ItemDetailField) : CellType()
    data class creator(val creator: ItemDetailCreator) : CellType()
    data class value(val value: String, val title: String) : CellType()
}
