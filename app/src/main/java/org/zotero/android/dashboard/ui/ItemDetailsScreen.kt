@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.dashboard.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.dashboard.ItemDetailsViewModel
import org.zotero.android.dashboard.ItemDetailsViewState
import org.zotero.android.formatter.dateFormatItemDetails
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CloseIconTopBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun ItemDetailsScreen(
    onBack: () -> Unit,
    viewModel: ItemDetailsViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(ItemDetailsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }
//
    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
        }
    }
    CustomScaffold(
        topBar = {
            TopBar(
                onCloseClicked = onBack,
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
                val itemResponse = viewState.itemResponse
                if (itemResponse != null) {
                    displayItem(itemResponse, layoutType)
                }
            }
        }

    }

}

@Composable
private fun displayItem(
    itemResponse: ItemResponse,
    layoutType: CustomLayoutSize.LayoutType
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {

        LazyColumn() {
            item {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 15.dp, start = 12.dp, end = 12.dp),
                    text = itemResponse.title ?: "No Title",
                    color = CustomTheme.colors.primaryContent,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTitleTextSize(),
                )

                CustomDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                        .height(2.dp)
                )
            }

            item {
                DetailRow(
                    detailTitle = stringResource(id = Strings.item_type),
                    detailValue = itemResponse.rawType.capitalize(),
                    layoutType = layoutType
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailRow(
                    detailTitle = stringResource(id = Strings.date_added),
                    detailValue = dateFormatItemDetails.format(itemResponse.dateAdded),
                    layoutType = layoutType
                )

                Spacer(modifier = Modifier.height(8.dp))

                DetailRow(
                    detailTitle = stringResource(id = Strings.date_modified),
                    detailValue = dateFormatItemDetails.format(itemResponse.dateModified),
                    layoutType = layoutType
                )

                CustomDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(2.dp)
                )
            }

            displayChildSection(
                sectionTitle = Strings.notes,
                icon = Drawables.item_note,
                childList = itemResponse.notes,
                layoutType = layoutType
            )

            displayChildSection(
                sectionTitle = Strings.tags,
                icon = Drawables.ic_tag,
                childList = itemResponse.tags.map { it.tag },
                layoutType = layoutType
            )


            displayChildSection(
                sectionTitle = Strings.attachments,
                icon = Drawables.attachment_list_pdf,
                childList = itemResponse.attachments,
                layoutType = layoutType
            )

        }

    }


}

private fun LazyListScope.displayChildSection(
    sectionTitle: Int,
    @DrawableRes icon: Int,
    childList: List<String>,
    layoutType: CustomLayoutSize.LayoutType
) {
    if (childList.isNotEmpty()) {

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = CustomTheme.colors.disabledContent)
                ) {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = stringResource(id = sectionTitle),
                        color = CustomTheme.colors.primaryContent,
                        style = CustomTheme.typography.default,
                        fontSize = layoutType.calculateTextSize(),
                    )
                }
            }
        }


        items(
            childList
        ) { item ->
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                    )

                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        text = androidx.core.text.HtmlCompat.fromHtml(
                            item,
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ).toString(),
                        fontSize = layoutType.calculateTextSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                CustomDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    detailTitle: String,
    detailValue: String,
    layoutType: CustomLayoutSize.LayoutType,
) {
    Row {
        Column(modifier = Modifier.width(140.dp)) {
            Text(
                modifier = Modifier.align(Alignment.End),
                text = detailTitle,
                color = CustomTheme.colors.secondaryContent,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                modifier = Modifier,
                text = detailValue,
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )
        }

    }
}

@Composable
private fun TopBar(
    onCloseClicked: () -> Unit,
) {
    CloseIconTopBar(
        title = stringResource(id = Strings.item_details),
        onClose = onCloseClicked,
    ) {
    }
}
