package org.zotero.android.screens.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.androidx.content.longToast
import org.zotero.android.screens.share.ShareViewEffect.NavigateBack
import org.zotero.android.screens.share.ShareViewEffect.NavigateToCollectionPickerScreen
import org.zotero.android.screens.share.ShareViewEffect.NavigateToTagPickerScreen
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun ShareScreen(
    navigateToTagPicker: () -> Unit,
    navigateToCollectionPicker: () -> Unit,
    onBack: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.topBarBackgroundColor
    val context = LocalContext.current
    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = backgroundColor,
        navBarBackgroundColor = backgroundColor
    ) {
        val viewState by viewModel.viewStates.observeAsState(ShareViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                NavigateBack -> onBack()
                NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }
                NavigateToCollectionPickerScreen-> {
                    navigateToCollectionPicker()
                }
                null -> Unit
            }
        }
        CustomScaffold(
            backgroundColor = backgroundColor,
            topBar = {
                ShareScreenTopBar(
                    onCancelClicked = onBack,
                    onSave = {
                        context.longToast("Not Implemented Yet")
                    },
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = backgroundColor)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    ParsedShareItemSection(
                        item = viewState.expectedItem,
                        attachment = viewState.expectedAttachment,
                        attachmentState = viewState.attachmentState,
                        title = viewState.title,
                        itemTitle = viewModel::itemTitle
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    CollectionSection(
                        collectionPickerState = viewState.collectionPickerState,
                        recents = viewState.recents,
                        navigateToMoreCollections = {
                            viewModel.navigateToCollectionPicker()
                        }, onCollectionClicked = { collection, library ->
                            viewModel.setFromRecent(collection, library)
                        })
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    TagsSection(
                        navigateToTagPicker = viewModel::navigateToTagPicker,
                        tags = viewState.tags
                    )
                }
            }
        }
    }
}
