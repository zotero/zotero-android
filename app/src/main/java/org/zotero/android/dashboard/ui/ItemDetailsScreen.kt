@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.dashboard.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.CustomLayoutSize.LayoutType
import org.zotero.android.dashboard.ItemDetailsViewEffect
import org.zotero.android.dashboard.ItemDetailsViewModel
import org.zotero.android.dashboard.ItemDetailsViewState
import org.zotero.android.dashboard.data.ItemDetailCreator
import org.zotero.android.dashboard.data.ItemDetailField
import org.zotero.android.formatter.dateFormatItemDetails
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CloseIconTopBar

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
                println()
            }
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
                displayItem(
                    viewState = viewState,
                    layoutType = layoutType,
                    viewModel = viewModel
                )
            }
        }

    }

}

@Composable
private fun displayItem(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    layoutType: LayoutType
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {

        LazyColumn() {
            item {
                Title(viewState, layoutType)
                CustomDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                        .height(2.dp)
                )
            }

            item {
                ItemType(viewState, layoutType)
                ListOfCreatorRows(viewState, layoutType)
                CustomDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
                AddItemRow(
                    layoutType = layoutType,
                    icon = Drawables.add_icon,
                    titleRes = Strings.add_creator,
                    onClick = viewModel::onAddCreator
                )
                ListOfFieldRows(viewState, layoutType)
                DatesRows(viewState, layoutType)
                CustomDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(2.dp)
                )
            }

            listOfNotesOrTags(
                sectionTitle = Strings.notes,
                icon = Drawables.item_note,
                itemTitles = viewState.notes.map { it.title },
                layoutType = layoutType
            )

            listOfNotesOrTags(
                sectionTitle = Strings.tags,
                icon = Drawables.ic_tag,
                itemTitles = viewState.tags.map { it.name },
                layoutType = layoutType
            )
            listOfAttachments(attachments = viewState.attachments, layoutType)

        }

    }

}

@Composable
private fun AddItemRow(
    titleRes: Int,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    layoutType: LayoutType
) {
    Column(
        modifier = Modifier
            .padding(start = 4.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(26.dp),
                painter = painterResource(id = icon),
                contentDescription = null,
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                text = stringResource(id = titleRes),
                fontSize = layoutType.calculateTextSize(),
                color = CustomTheme.colors.zoteroBlueWithDarkMode,
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

@Composable
private fun ItemType(viewState: ItemDetailsViewState, layoutType: LayoutType) {
    FieldRow(stringResource(id = Strings.item_type), viewState.data.localizedType, layoutType)
}

@Composable
private fun DatesRows(viewState: ItemDetailsViewState, layoutType: LayoutType) {
    FieldRow(
        stringResource(id = Strings.date_added),
        dateFormatItemDetails.format(viewState.data.dateAdded),
        layoutType
    )
    FieldRow(
        stringResource(id = Strings.date_modified),
        dateFormatItemDetails.format(viewState.data.dateModified),
        layoutType
    )
}

@Composable
private fun ListOfCreatorRows(viewState: ItemDetailsViewState, layoutType: LayoutType) {
    for (creatorId in viewState.data.creatorIds) {
        val creator = viewState.data.creators.get(creatorId) ?: continue
        val title = creator.localizedType
        val value = creator.name
        FieldRow(title, value, layoutType)
    }

}

@Composable
private fun ListOfFieldRows(
    viewState: ItemDetailsViewState,
    layoutType: LayoutType
) {
    for (fieldId in viewState.data.fieldIds) {
        val field = viewState.data.fields.get(fieldId) ?: continue
        val title = field.name
        var value = field.additionalInfo?.get(ItemDetailField.AdditionalInfoKey.formattedDate)
            ?: field.value
        value = if (value.isEmpty()) " " else value
        val textColor = if (field.isTappable) {
            CustomPalette.Blue
        } else {
            CustomTheme.colors.primaryContent
        }
        FieldRow(title, value, layoutType, textColor)
    }
}

@Composable
private fun FieldRow(
    detailTitle: String,
    detailValue: String,
    layoutType: LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
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
                color = textColor,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ColumnScope.Title(
    viewState: ItemDetailsViewState,
    layoutType: LayoutType
) {
    Text(
        modifier = Modifier.Companion
            .align(Alignment.CenterHorizontally)
            .padding(bottom = 15.dp, start = 12.dp, end = 12.dp),
        text = viewState.data.title,
        color = CustomTheme.colors.primaryContent,
        style = CustomTheme.typography.default,
        fontSize = layoutType.calculateTitleTextSize(),
    )
}

private fun LazyListScope.listOfNotesOrTags(
    sectionTitle: Int,
    @DrawableRes icon: Int,
    itemTitles: List<String>,
    layoutType: LayoutType
) {
    if (itemTitles.isNotEmpty()) {

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
            itemTitles
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

private fun LazyListScope.listOfAttachments(
    attachments: List<Attachment>,
    layoutType: LayoutType
) {
    if (attachments.isNotEmpty()) {
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
                        text = stringResource(id = Strings.attachments),
                        color = CustomTheme.colors.primaryContent,
                        style = CustomTheme.typography.default,
                        fontSize = layoutType.calculateTextSize(),
                    )
                }
            }
        }


        items(
            attachments
        ) { item ->
            Column(modifier = Modifier.padding(start = 4.dp)) {
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
                        text = androidx.core.text.HtmlCompat.fromHtml(
                            item.title,
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
private fun TopBar(
    onCloseClicked: () -> Unit,
) {
    CloseIconTopBar(
        title = stringResource(id = Strings.item_details),
        onClose = onCloseClicked,
    )
}

private sealed class CellType {
    data class field(val field: ItemDetailField) : CellType()
    data class creator(val creator: ItemDetailCreator) : CellType()
    data class value(val value: String, val title: String) : CellType()
}
