package org.zotero.android.screens.itemdetails

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.reorder.ReorderableState
import org.zotero.android.uicomponents.reorder.draggedItem
import org.zotero.android.uicomponents.reorder.reorderable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ItemDetailsEditScreen(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    reorderState: ReorderableState,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .reorderable(
                state = reorderState,
                onMove = viewModel::onMove,
            ),
        state = reorderState.listState,
    ) {
        item {
            EditTitle(viewState, onValueChange = viewModel::onTitleEdit)
            Spacer(modifier = Modifier.height(12.dp))
            CustomDivider(modifier = Modifier.padding(start = 16.dp))
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                ItemType(
                    viewState = viewState,
                    layoutType = layoutType,
                    onItemTypeClicked = viewModel::onItemTypeClicked
                )
            }
        }
        if (!viewState.data.isAttachment) {
            listOfCreatorRows(
                viewState = viewState,
                layoutType = layoutType,
                onDeleteCreator = viewModel::onDeleteCreator,
                onCreatorClicked = viewModel::onCreatorClicked,
                reorderState = reorderState,
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (!viewState.data.isAttachment) {
                    CustomDivider()
                    AddItemRow(
                        titleRes = Strings.item_detail_add_creator,
                        startPadding = 0.dp,
                        onClick = viewModel::onAddCreator
                    )
                    CustomDivider()
                }

                ListOfEditFieldRows(
                    viewState = viewState,
                    layoutType = layoutType,
                    onValueChange = viewModel::onFieldValueTextChange,
                    onFocusChanges = viewModel::onFieldFocusFieldChange
                )
                DatesRows(
                    dateAdded = viewState.data.dateAdded,
                    dateModified = viewState.data.dateModified,
                    layoutType = layoutType,
                    showDivider = true
                )
                if (!viewState.data.isAttachment) {
                    EditAbstractRow(
                        detailValue = viewState.abstractText,
                        onValueChange = viewModel::onAbstractTextChange
                    )
                }
            }
        }
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
                indication = ripple(),
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

private fun LazyListScope.listOfCreatorRows(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    reorderState: ReorderableState,
    onDeleteCreator: (String) -> Unit,
    onCreatorClicked: (ItemDetailCreator) -> Unit,
) {
    for ((index, creatorId) in viewState.data.creatorIds.withIndex()) {
        val creator = viewState.data.creators[creatorId] ?: continue
        item {
            val title = creator.localizedType
            val value = creator.name
            Row(
                modifier = Modifier
                    .safeClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { onCreatorClicked(creator) }
                    )
                    .draggedItem(reorderState.offsetByIndex(index + numberOfRowsInLazyColumnBeforeListOfCreatorsStarts))
                    .background(color = CustomTheme.colors.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldRow(
                    detailTitle = title,
                    detailValue = value,
                    layoutType = layoutType,
                    showDivider = true,
                    reorderState = reorderState,
                    onDelete = { onDeleteCreator(creatorId) }
                )
            }
        }
    }
}

@Composable
private fun ListOfEditFieldRows(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onValueChange: (String, String) -> Unit,
    onFocusChanges: (String) -> Unit,
) {
    for (fieldId in viewState.data.fieldIds) {
        val field = viewState.data.fields.get(fieldId) ?: continue
        val title = field.name
        val value = if (field.key == viewState.fieldFocusKey) {
            viewState.fieldFocusText
        } else {
            field.valueOrAdditionalInfo
        }
        FieldEditableRow(
            key = field.key,
            fieldId = fieldId,
            detailTitle = title,
            detailValue = value,
            layoutType = layoutType,
            textColor = CustomTheme.colors.primaryContent,
            onValueChange = onValueChange,
            isMultilineAllowed = field.key == FieldKeys.Item.extra,
            onFocusChanges = onFocusChanges
        )
    }
}

@Composable
private fun FieldEditableRow(
    key: String,
    fieldId: String,
    detailTitle: String,
    detailValue: String,
    layoutType: CustomLayoutSize.LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    isMultilineAllowed: Boolean = false,
    onValueChange: (String, String) -> Unit,
    onFocusChanges: (String) -> Unit,
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
                    style = CustomTheme.typography.newHeadline,
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                if (isMultilineAllowed) {
                    CustomTextField(
                        modifier = Modifier
                            .fillMaxSize()
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    onFocusChanges(fieldId)
                                }
                            },
                        value = detailValue,
                        hint = "",
                        textColor = textColor,
                        onValueChange = { onValueChange(key, it) },
                        textStyle = CustomTheme.typography.newBody,
                        ignoreTabsAndCaretReturns = false,
                    )
                } else {
                    val focusManager = LocalFocusManager.current
                    val moveFocusDownAction = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                    CustomTextField(
                        modifier = Modifier
                            .fillMaxSize()
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    onFocusChanges(fieldId)
                                }
                            },
                        value = detailValue,
                        hint = "",
                        textColor = textColor,
                        maxLines = 1,
                        onValueChange = { onValueChange(key, it) },
                        textStyle = CustomTheme.typography.newBody,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { moveFocusDownAction() }
                        ),
                        onEnterOrTab = { moveFocusDownAction() },
                    )
                }

            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CustomDivider()
    }
}

@Composable
private fun EditTitle(
    viewState: ItemDetailsViewState,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val moveFocusDownAction = {
        focusManager.moveFocus(FocusDirection.Down)
    }
    CustomTextField(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        value = viewState.data.title,
        hint = stringResource(id = Strings.item_detail_untitled),
        onValueChange = onValueChange,
        textStyle = CustomTheme.typography.newTitleOne,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { moveFocusDownAction() }
        ),
        onEnterOrTab = { moveFocusDownAction() },
    )
}

@Composable
private fun EditAbstractRow(
    detailValue: String,
    textColor: Color = CustomTheme.colors.primaryContent,
    onValueChange: (String) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.align(Alignment.Start),
            text = stringResource(id = Strings.abstract_1),
            color = CustomTheme.colors.secondaryContent,
            style = CustomTheme.typography.newHeadline,
        )
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(
            modifier = Modifier
                .fillMaxSize(),
            value = detailValue,
            hint = "",
            textColor = textColor,
            onValueChange = onValueChange,
            textStyle = CustomTheme.typography.newBody,
            ignoreTabsAndCaretReturns = false,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
