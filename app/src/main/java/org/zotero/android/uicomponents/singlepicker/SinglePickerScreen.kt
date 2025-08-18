package org.zotero.android.uicomponents.singlepicker

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
fun SinglePickerScreen(
    onCloseClicked: () -> Unit,
) {
    CustomThemeWithStatusAndNavBars {
        val pickerArgs = ScreenArguments.singlePickerArgs
        val singlePickerState = pickerArgs.singlePickerState
        val layoutType = CustomLayoutSize.calculateLayoutType()
        CustomScaffold(
            topBarColor = CustomTheme.colors.topBarBackgroundColor,
            topBar = {
                SinglePickerTopBar(
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
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .safeClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!layoutType.isTablet() || pickerArgs.callPoint != SinglePickerResult.CallPoint.AllItemsShowItem) {
                                        onCloseClicked()
                                    }
                                    EventBus
                                        .getDefault()
                                        .post(SinglePickerResult(option.id, pickerArgs.callPoint))
                                }
                            )
                    ) {
                        Row(modifier = Modifier.align(Alignment.CenterStart)) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = option.name,
                                style = CustomTheme.typography.newBody,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = CustomTheme.colors.primaryContent,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (option.id == singlePickerState.selectedRow) {
                                Icon(
                                    painter = painterResource(id = Drawables.check_24px),
                                    contentDescription = null,
                                    tint = CustomTheme.colors.zoteroDefaultBlue
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
                        NewDivider(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = 16.dp)
                        )
                    }
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