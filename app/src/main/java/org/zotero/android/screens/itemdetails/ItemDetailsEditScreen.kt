package org.zotero.android.screens.itemdetails

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.screens.itemdetails.data.ItemDetailField
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme
import java.util.UUID

@Composable
internal fun ItemDetailsEditScreen(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        item {
            EditTitle(viewState, layoutType, onValueChange = viewModel::onTitleEdit)
            CustomDivider()
        }
        item {
            Column(modifier = Modifier.padding(start = 12.dp)) {
                ItemType(
                    viewState = viewState,
                    layoutType = layoutType,
                    onItemTypeClicked = viewModel::onItemTypeClicked
                )
                if (!viewState.data.isAttachment) {
                    ListOfCreatorRows(
                        viewState = viewState,
                        layoutType = layoutType,
                        onDeleteCreator = viewModel::onDeleteCreator,
                        onCreatorClicked = viewModel::onCreatorClicked
                    )
                    CustomDivider()
                    AddItemRow(
                        layoutType = layoutType,
                        titleRes = Strings.add_creator,
                        onClick = viewModel::onAddCreator
                    )
                    CustomDivider()
                }

                ListOfEditFieldRows(viewState, layoutType, viewModel::setFieldValue)
                DatesRows(
                    dateAdded = viewState.data.dateAdded,
                    dateModified = viewState.data.dateModified,
                    layoutType = layoutType,
                    showDivider = true
                )
                if (!viewState.data.isAttachment) {
                    EditAbstractRow(
                        detailValue = viewState.data.abstract ?: "",
                        layoutType = layoutType, onValueChange = viewModel::onAbstractEdit
                    )
                }
            }
        }
        notesTagsAndAttachmentsBlock(
            viewState = viewState,
            viewModel = viewModel,
            layoutType = layoutType,
            onNoteClicked = { viewModel.openNoteEditor(it) },
            onAddNote = { viewModel.onAddNote() },
        )
    }
}

@Composable
private fun ItemType(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onItemTypeClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onItemTypeClicked
            )
    ) {
        Column(
            modifier = Modifier.padding(start = 28.dp + layoutType.calculateItemCreatorDeleteStartPadding())
        ) {
            FieldRow(
                detailTitle = stringResource(id = Strings.item_type),
                detailValue = viewState.data.localizedType,
                layoutType = layoutType,
                showDivider = false
            )
        }
        CustomDivider()
    }
}

@Composable
private fun ListOfCreatorRows(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onDeleteCreator: (UUID) -> Unit,
    onCreatorClicked: (ItemDetailCreator) -> Unit,
) {
    for (creatorId in viewState.data.creatorIds) {
        val creator = viewState.data.creators.get(creatorId) ?: continue
        val title = creator.localizedType
        val value = creator.name
        Row(
            modifier = Modifier
                .safeClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(),
                    onClick = { onCreatorClicked(creator) }
                )
        ) {
            FieldRow(
                detailTitle = title,
                detailValue = value,
                layoutType = layoutType,
                showDivider = true,
                onDelete = { onDeleteCreator(creatorId) }
            )
        }

    }
}

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
                    .width(layoutType.calculateItemFieldLabelWidth())
            ) {
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = detailTitle,
                    overflow =TextOverflow.Ellipsis,
                    maxLines = 1,
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
                    textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CustomDivider()
    }
}

@Composable
private fun EditTitle(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onValueChange: (String) -> Unit,
) {
    CustomTextField(
        modifier = Modifier
            .padding(bottom = 12.dp, end = 12.dp, start = 12.dp),
        value = viewState.data.title,
        hint = stringResource(id = Strings.untitled),
        onValueChange = onValueChange,
        textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTitleTextSize())
    )
}

@Composable
private fun EditAbstractRow(
    detailValue: String,
    layoutType: CustomLayoutSize.LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    onValueChange: (String) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.align(Alignment.Start),
            text = stringResource(id = Strings.abstractS),
            color = CustomTheme.colors.secondaryContent,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculateTextSize(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(
            modifier = Modifier
                .fillMaxSize(),
            value = detailValue,
            hint = "",
            textColor = textColor,
            onValueChange = onValueChange,
            textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
