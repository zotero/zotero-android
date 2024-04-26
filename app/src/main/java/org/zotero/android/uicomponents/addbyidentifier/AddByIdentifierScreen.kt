package org.zotero.android.uicomponents.addbyidentifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
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
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun AddByIdentifierScreen(
    viewModel: AddByIdentifierViewModel = hiltViewModel(),
    onCancel: () -> Unit,
) {
    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground
    ) {
        val viewState by viewModel.viewStates.observeAsState(AddByIdentifierViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is AddByIdentifierViewEffect.NavigateBack -> {
                    onCancel()
                }

                else -> {}
            }
        }
        CustomScaffold(
            topBar = {
                AddByIdentifierTopBar(
                    title = null,
                    onCancel = onCancel,
                    onLookup = viewModel::onLookup
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    IsbnTitle()

                    Spacer(modifier = Modifier.height(12.dp))
                    IsbnEditField(
                        isbnText = viewState.isbnText,
                        onIsbnTextChange = viewModel::onIsbnTextChange,
                    )
                }
            }
        }
    }
}

@Composable
fun IsbnTitle() {
    Text(
        text = stringResource(id = Strings.lookup_title),
        color = CustomPalette.DarkGrayColor,
        style = CustomTheme.typography.subhead,
    )
}

@Composable
internal fun IsbnEditField(
    isbnText: String,
    onIsbnTextChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
//            .height(44.dp)
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = RoundedCornerShape(size = 10.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        CustomTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            value = isbnText,
            hint = "",
            minLines = 4,
            maxLines = 4,
            ignoreTabsAndCaretReturns = false,
            focusRequester = focusRequester,
            textColor = CustomTheme.colors.primaryContent,
            onValueChange = onIsbnTextChange,
            textStyle = CustomTheme.typography.newBody,
        )
    }
}