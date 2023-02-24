package org.zotero.android.uicomponents.singlepicker

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.row.BaseRowItem
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
fun SinglePickerScreen(
    onCloseClicked: () -> Unit,
    scaffoldModifier: Modifier = Modifier,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val pickerArgs = ScreenArguments.singlePickerArgs
    val singlePickerState = pickerArgs.singlePickerState
    CustomScaffold(
        modifier = scaffoldModifier,
        topBar = {
            TopBar(
                title = pickerArgs.title,
                onCancel = onCloseClicked
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(
                singlePickerState.objects
            ) { option ->
                Column(modifier = Modifier
                    .safeClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onCloseClicked()
                            EventBus.getDefault().post(SinglePickerResult(option.id, pickerArgs.callPoint))
                        }
                    )) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BaseRowItem(
                        modifier = Modifier.padding(start = 16.dp),
                        title = option.name,
                        heightIn = 24.dp,
                        startContentPadding = 12.dp,
                        textColor = CustomTheme.colors.primaryContent,
                        titleStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
                        endContent = {
                            if (option.id == singlePickerState.selectedRow) {
                                Icon(
                                    modifier = Modifier.size(layoutType.calculateIconSize()),
                                    painter = painterResource(id = Drawables.baseline_check_24),
                                    contentDescription = null,
                                    tint = CustomTheme.colors.zoteroBlueWithDarkMode
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                        })
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomDivider()
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String?,
    onCancel: () -> Unit,
) {
    CancelSaveTitleTopBar(
        title = title,
        onCancel = onCancel,
    )
}


data class SinglePickerState(
    val objects: List<SinglePickerItem>,
    val selectedRow: String,
)

data class SinglePickerItem(
    val id: String,
    val name: String,
)