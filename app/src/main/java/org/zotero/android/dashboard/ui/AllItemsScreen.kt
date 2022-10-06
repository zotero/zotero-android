@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.dashboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.dashboard.AllItemsViewModel
import org.zotero.android.dashboard.AllItemsViewState
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CloseIconTopBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun AllItemsScreen(
    navigateToItemDetails: (itemId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: AllItemsViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(AllItemsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(key1 = viewModel) {
        viewModel.init(lifecycleOwner = lifecycleOwner)
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
        }
    }

    CustomScaffold(
        topBar = {
            TopBar(onCloseClicked = onBack)
        },
    ) {
        BaseLceBox(
            modifier = Modifier.fillMaxSize(),
            lce = viewState.lce,
            error = { lceError ->
                FullScreenError(
                    modifier = Modifier.align(Alignment.Center),
                    errorTitle = stringResource(id = Strings.all_items_load_error),
                )
            },
            loading = {
                CircularLoading()
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = rememberLazyListState(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(
                            viewState.items
                        ) { item ->
                            ItemView(
                                modifier = Modifier.safeClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = rememberRipple(),
                                    onClick = {
                                        navigateToItemDetails(item.key)
                                    }
                                ),
                                item = item,
                                layoutType = layoutType
                            )
                        }
                    }

                }
            }

        }

    }

}

@Composable
private fun ItemView(
    modifier: Modifier,
    item: ItemResponse,
    layoutType: CustomLayoutSize.LayoutType
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = Drawables.attachment_list_pdf),
                contentDescription = null,
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                text = item.title ?: "Some ${item.rawType}",
                fontSize = layoutType.calculateTextSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                painter = painterResource(id = Drawables.ic_exclamation_24dp),
                contentDescription = null,
                tint = CustomPalette.Blue
            )
        }


        CustomDivider(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        )
    }


}

@Composable
private fun TopBar(
    onCloseClicked: () -> Unit,
) {
    CloseIconTopBar(
        title = stringResource(id = Strings.all_items),
        onClose = onCloseClicked,
    ) {
    }
}