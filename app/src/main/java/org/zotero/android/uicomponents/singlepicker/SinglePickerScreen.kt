package org.zotero.android.uicomponents.singlepicker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
fun SinglePickerScreen(
    onCloseClicked: () -> Unit,
) {

    AppThemeM3 {
        val pickerArgs = ScreenArguments.singlePickerArgs
        val singlePickerState = pickerArgs.singlePickerState
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                SinglePickerTopBar(
                    title = pickerArgs.title,
                    scrollBehavior = scrollBehavior,
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
                    SinglePickerRadioButtonRow(
                        text = option.name,
                        isSelected = option.id == singlePickerState.selectedRow,
                        onOptionSelected = {
                            if (!layoutType.isTablet() || pickerArgs.callPoint != SinglePickerResult.CallPoint.AllItemsShowItem) {
                                onCloseClicked()
                            }
                            EventBus
                                .getDefault()
                                .post(SinglePickerResult(option.id, pickerArgs.callPoint))
                        })
                }
                item {
                    Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
                }
            }
        }
    }
}

data class SinglePickerState(
    val objects: List<SinglePickerItem>,
    val selectedRow: String,
)

data class SinglePickerItem(
    val id: String,
    val name: String,
)