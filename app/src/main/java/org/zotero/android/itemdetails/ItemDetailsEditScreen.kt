package org.zotero.android.itemdetails

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.dashboard.data.ItemDetailField
import org.zotero.android.formatter.dateFormatItemDetails
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ItemDetailsEditScreen(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 16.dp)
    ) {

        LazyColumn {
            item {
                EditTitle(viewState, layoutType, onValueChange = viewModel::onTitleEdit)
                CustomDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(2.dp)
                )
            }

            item {
                ItemType(viewState, layoutType)
//                ListOfCreatorRows(viewState, layoutType)
//                CustomDivider(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(2.dp)
//                )
                AddItemRow(
                    layoutType = layoutType,
                    icon = Drawables.add_icon,
                    titleRes = Strings.add_creator,
                    onClick = viewModel::onAddCreator
                )
                CustomDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
                ListOfEditFieldRows(viewState, layoutType, viewModel::setFieldValue)
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
    layoutType: CustomLayoutSize.LayoutType
) {
    Column(
        modifier = Modifier
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick
            )
    ) {
        Spacer(modifier = Modifier.height(8.dp))
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
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ItemType(viewState: ItemDetailsViewState, layoutType: CustomLayoutSize.LayoutType) {
    FieldRow(
        detailTitle = stringResource(id = Strings.item_type),
        detailValue = viewState.data.localizedType,
        layoutType = layoutType,
        showDivider = true
    )
}

@Composable
private fun DatesRows(viewState: ItemDetailsViewState, layoutType: CustomLayoutSize.LayoutType) {
    FieldRow(
        detailTitle = stringResource(id = Strings.date_added),
        detailValue = dateFormatItemDetails.format(viewState.data.dateAdded),
        layoutType = layoutType,
        showDivider = true
    )
    FieldRow(
        detailTitle = stringResource(id = Strings.date_modified),
        detailValue = dateFormatItemDetails.format(viewState.data.dateModified),
        layoutType = layoutType,
        showDivider = true
    )
}

//@Composable
//private fun ListOfCreatorRows(viewState: ItemDetailsViewState, layoutType: CustomLayoutSize.LayoutType) {
//    for (creatorId in viewState.data.creatorIds) {
//        val creator = viewState.data.creators.get(creatorId) ?: continue
//        val title = creator.localizedType
//        val value = creator.name
//        FieldRow(title, value, layoutType)
//    }
//}

@Composable
private fun ListOfEditFieldRows(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onValueChange: (String, String) -> Unit,
) {
    for (fieldId in viewState.data.fieldIds) {
        val field = viewState.data.fields.get(fieldId) ?: continue
        val title = field.name
        val value = field.additionalInfo?.get(ItemDetailField.AdditionalInfoKey.formattedDate)
            ?: field.value
        FieldEditableRow(
            key = field.key,
            detailTitle = title,
            detailValue = value,
            layoutType = layoutType,
            textColor = CustomTheme.colors.primaryContent,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun FieldEditableRow(
    key: String,
    detailTitle: String,
    detailValue: String,
    layoutType: CustomLayoutSize.LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    onValueChange: (String, String) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Column(
                modifier = Modifier
                    .width(140.dp)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = detailTitle,
                    color = CustomTheme.colors.secondaryContent,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                CustomTextField(
                    modifier = Modifier
                        .fillMaxSize(),
                    value = detailValue,
                    hint = "",
                    textColor = textColor,
                    maxLines = 1,
                    onValueChange = { onValueChange(key, it) },
                    textStyle = CustomTheme.typography.default,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CustomDivider(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        )
    }
}

@Composable
private fun ColumnScope.EditTitle(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onValueChange: (String) -> Unit,
) {
    CustomTextField(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = 15.dp, end = 12.dp),
        value = viewState.data.title,
        hint = stringResource(id = Strings.untitled),
        maxLines = 1,
        onValueChange = onValueChange,
        textStyle = CustomTheme.typography.default,
    )
}

private fun LazyListScope.listOfNotesOrTags(
    sectionTitle: Int,
    @DrawableRes icon: Int,
    itemTitles: List<String>,
    layoutType: CustomLayoutSize.LayoutType
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
                        text = HtmlCompat.fromHtml(
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
    layoutType: CustomLayoutSize.LayoutType
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
                        text = HtmlCompat.fromHtml(
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