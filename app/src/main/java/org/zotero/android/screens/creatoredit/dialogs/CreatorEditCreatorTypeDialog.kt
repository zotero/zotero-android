package org.zotero.android.screens.creatoredit.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.zotero.android.screens.creatoredit.CreatorEditViewModel
import org.zotero.android.screens.creatoredit.CreatorEditViewState
import org.zotero.android.uicomponents.Strings

@Composable
internal fun CreatorEditCreatorTypeDialog(
    viewModel: CreatorEditViewModel,
    viewState: CreatorEditViewState
) {
    Dialog(onDismissRequest = viewModel::dismissChooserDialog) {
        val roundCornerShape = RoundedCornerShape(size = 30.dp)
        Column(
            Modifier
                .wrapContentSize()
                .clip(roundCornerShape)
                .background(MaterialTheme.colorScheme.surface)
                .selectableGroup()
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(
                        text = stringResource(Strings.creator_editor_creator),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
            )

            viewState.listOfCreatorTypes.forEach {
                CreatorTypeRadioButton(
                    text = it.name,
                    isSelected = viewState.selectedCreatorType == it.id,
                    onOptionSelected = { viewModel.onCreatorTypeSelected(it.id) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }


    }
}

@Composable
private fun CreatorTypeRadioButton(
    text: String,
    isSelected: Boolean,
    onOptionSelected: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .selectable(
                selected = isSelected,
                onClick = onOptionSelected,
                role = Role.RadioButton,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
