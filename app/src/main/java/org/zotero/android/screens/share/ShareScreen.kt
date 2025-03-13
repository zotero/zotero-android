package org.zotero.android.screens.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.androidx.content.getDrawableByItemType
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
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
        val isSubmitting = viewState.isSubmitting
        val isRetrieveMetadataLoading = viewState.retrieveMetadataState is RetrieveMetadataState.loading
        CustomScaffold(
            backgroundColor = backgroundColor,
            topBar = {
                ShareScreenTopBar(
                    onCancelClicked = onBack,
                    onSave = {
                        viewModel.submitAsync()
                    },
                    isLeftButtonEnabled = !isSubmitting,
                    isRightButtonEnabled = !isSubmitting && viewState.attachmentState.isSubmittable && !isRetrieveMetadataLoading,
                    attachmentError = viewState.attachmentState.error,
                    isLoading = isSubmitting || isRetrieveMetadataLoading,
                    state = viewState.attachmentState,
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
                    val retrieveMetadataState = viewState.retrieveMetadataState
                    if (retrieveMetadataState is RetrieveMetadataState.success) {
                        RecognizeItemSection(retrieveMetadataState)
                    } else {
                        ParsedShareItemSection(
                            item = viewState.expectedItem,
                            attachment = viewState.expectedAttachment,
                            attachmentState = viewState.attachmentState,
                            title = viewState.title,
                            itemTitle = viewModel::itemTitle
                        )
                    }

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
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    val hasItem = viewState.processedAttachment != null
                    ShareFailureBottomPanel(
                        viewModel = viewModel,
                        state = viewState.attachmentState,
                        itemState = viewState.itemPickerState,
                        hasItem = hasItem,
                        isSubmitting = isSubmitting
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognizeItemSection(retrieveMetadataState: RetrieveMetadataState.success) {
    Spacer(modifier = Modifier.height(20.dp))
    ShareSection {
        RecognizedItemRow(
            title = retrieveMetadataState.recognizedTitle,
            iconSize = 28.dp,
            typeIconName = retrieveMetadataState.recognizedTypeIcon
        )
        Row {
            Spacer(modifier = Modifier.width(16.dp))
            RecognizedItemRow(
                title = "PDF",
                iconSize = 22.dp,
                typeIconName = ItemTypes.iconName(
                    rawType = ItemTypes.attachment,
                    contentType = "pdf"
                )
            )
        }
    }
}

@Composable
private fun RecognizedItemRow(
    iconSize: Dp,
    title: String,
    typeIconName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            modifier = Modifier.size(iconSize),
            painter = painterResource(id = LocalContext.current.getDrawableByItemType(typeIconName)),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = HtmlCompat.fromHtml(
                title,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toString(),
            style = CustomTheme.typography.newBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

}
