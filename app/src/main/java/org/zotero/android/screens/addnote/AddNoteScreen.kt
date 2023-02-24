
package org.zotero.android.screens.addnote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CustomIconTopBar
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun AddNoteScreen(
    onBack: () -> Unit,
    viewModel: AddNoteViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(AddNoteViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    val bodyFocusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            is AddNoteViewEffect.NavigateBack -> onBack()
            null -> Unit
        }
    }

    CustomScaffold(
        topBar = {
            AddNoteTopBar(titleData = viewState.title, onDoneClicked = viewModel::onDoneClicked)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.surface),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {

            Body(
                layoutType = layoutType,
                text = viewState.text,
                focusRequester = bodyFocusRequester,
                onValueChange = viewModel::onBodyTextChange
            )

        }

    }

}

@Composable
private fun Body(
    layoutType: CustomLayoutSize.LayoutType,
    text: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
    ) {
        CustomTextField(
            modifier = Modifier
                .fillMaxSize(),
            value = text,
            hint = "",
            focusRequester = focusRequester,
            onValueChange = onValueChange,
            textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
        )
    }
}

@Composable
private fun AddNoteTopBar(
    titleData: AddOrEditNoteArgs.TitleData?,
    onDoneClicked: () -> Unit,
) {
    CustomIconTopBar(
        title = titleData?.title,
        iconInt = titleData?.type?.let { ItemTypes.iconName(it, null) },
        actions = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onDoneClicked,
                text = stringResource(Strings.done)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    )

}