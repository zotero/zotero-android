package org.zotero.android.architecture.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.lang.Integer.min

object CustomLayoutSize {
    enum class LayoutType {
        big, medium, small;

        fun calculateItemCreatorDeleteStartPadding(): Dp {
            return when (this) {
                big, medium -> 18.dp
                small -> 11.dp
            }
        }

        fun calculateIconSize(): Dp {
            return when (this) {
                big, medium -> 30.dp
                small -> 22.dp
            }
        }

        fun calculateTextSize(): TextUnit {
            return when (this) {
                big, medium -> 20.sp
                small -> 14.sp
            }
        }

        fun calculateItemFieldLabelWidth(): Dp {
            return when (this) {
                big, medium -> 160.dp
                small -> 120.dp
            }
        }

        fun calculateTitleTextSize(): TextUnit {
            return when (this) {
                big, medium -> 26.sp
                small -> 20.sp
            }
        }

        fun calculatePadding(): Dp {
            return when (this) {
                big, medium -> 50.dp
                small -> 8.dp
            }
        }

        fun showScreenOrDialog(): ScreenOrDialogToShow {
            return when (this) {
                LayoutType.big, LayoutType.medium -> ScreenOrDialogToShow.DIALOG
                LayoutType.small -> ScreenOrDialogToShow.SCREEN
            }
        }

        fun calculateMainLoaderIconSize(): Dp {
            return when (this) {
                big, medium -> 100.dp
                small -> 60.dp
            }
        }

        fun calculateItemsRowInfoIconSize(): Dp {
            return when (this) {
                big, medium -> 50.dp
                small -> 36.dp
            }
        }

        fun calculateItemsRowNoteIconSize(): Dp {
            return when (this) {
                big, medium -> 14.dp
                small -> 10.dp
            }
        }

        fun calculateItemsRowAccessoryIconSize(): Dp {
            return when (this) {
                big, medium -> 22.dp
                small -> 18.dp
            }
        }

        fun calculateItemsRowMainIconSize(): Dp {
            return when (this) {
                big, medium -> 26.dp
                small -> 22.dp
            }
        }
        fun calculateItemsRowTextSize(): TextUnit {
            return when (this) {
                big, medium -> 20.sp
                small -> 14.sp
            }
        }

    }

    @Composable
    fun calculateLayoutType(): LayoutType {
        val configuration = LocalConfiguration.current

        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp

        val size = min(screenWidth, screenHeight).dp
        if (size >= 834.dp) {
            return LayoutType.big
        } else if (size >= 768.dp) {
            return LayoutType.medium
        }
        return LayoutType.small
    }
    enum class ScreenOrDialogToShow {
        SCREEN, DIALOG
    }
}
