package org.zotero.android.screens.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.filter.FilterScreenTablet
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun CollectionsScreen(
    onBack: () -> Unit,
    navigateToAllItems: (String) -> Unit,
    navigateToLibraries: (String) -> Unit,
    navigateToCollectionEdit: () -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    AppThemeM3 {

        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(CollectionsViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        val isTablet = layoutType.isTablet()

        BackHandler(
            enabled = viewState.backHandlerEnabled,
            onBack = {
                viewModel.navigateToLibraries()
            })

        LaunchedEffect(key1 = viewModel) {
            viewModel.init(isTablet = isTablet)
        }

        LaunchedEffect(key1 = viewEffect) {
            val consumedEffect = viewEffect?.consume()
            when (consumedEffect) {
                null -> Unit
                CollectionsViewEffect.NavigateBack -> onBack()
                is CollectionsViewEffect.NavigateToAllItemsScreen -> navigateToAllItems(
                    consumedEffect.screenArgs
                )

                CollectionsViewEffect.ShowCollectionEditEffect -> {
                    navigateToCollectionEdit()
                }

                is CollectionsViewEffect.NavigateToLibrariesScreen -> {
                    navigateToLibraries(consumedEffect.screenArgs)
                }

                else -> {}
            }
        }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                CollectionsTopBar(
                    libraryName = viewState.libraryName,
                    scrollBehavior = scrollBehavior,
                    navigateToLibraries = viewModel::navigateToLibraries,
                    onAdd = viewModel::onAdd,
                )
            },
        ) {
            BaseLceBox(
                modifier = Modifier.fillMaxSize(),
                lce = viewState.lce,
                error = { _ ->
                    FullScreenError(
                        modifier = Modifier.align(Alignment.Center),
                        errorTitle = stringResource(id = Strings.error_list_load_check_crash_logs),
                    )
                },
                loading = {
                    CircularLoading()
                },
            ) {
                Column {
                    if (!isTablet) {
                        CollectionsTable(
                            viewState = viewState,
                            viewModel = viewModel,
                            layoutType = layoutType
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxHeight(0.7f)) {
                            CollectionsTable(
                                viewState = viewState,
                                viewModel = viewModel,
                                layoutType = layoutType
                            )
                        }
                        NewDivider()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            val args = viewState.tabletFilterArgs
                            if (args != null) {
                                FilterScreenTablet(args)
                            }
                        }
                    }
                }
            }
        }
    }
}