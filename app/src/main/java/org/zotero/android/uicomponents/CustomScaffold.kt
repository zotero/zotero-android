package org.zotero.android.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.zotero.android.uicomponents.snackbar.CustomSnackbarHost
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CustomScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarMessage: SnackbarMessage? = null,
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    topBarColor: Color = CustomTheme.colors.surface,
    bottomBarColor: Color = CustomTheme.colors.surface,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            val modifierTopBar = Modifier
                .background(topBarColor)
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            Box(modifier = modifierTopBar) {
                topBar()
            }
        },
        bottomBar = bottomBar,
        snackbarHost = {
            CustomSnackbarHost(
                state = snackbarHostState,
                snackbarMessage = snackbarMessage
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = CustomTheme.colors.surface,
        content = { it ->
            Box(modifier = Modifier.background(bottomBarColor)) {
                Box(modifier = Modifier
                    .padding(it)
                    .background(CustomTheme.colors.surface)
                ) {
                    content(it)
                }
            }
        },
    )
}
