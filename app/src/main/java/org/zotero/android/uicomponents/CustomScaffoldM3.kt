package org.zotero.android.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.zotero.android.uicomponents.snackbar.CustomSnackbarHost
import org.zotero.android.uicomponents.snackbar.SnackbarMessage

@Composable
fun CustomScaffoldM3(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    snackbarMessage: SnackbarMessage? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    topBarColor: Color = MaterialTheme.colorScheme.surface,
    containerColor: Color = MaterialTheme.colorScheme.surface,
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
        containerColor = containerColor,
        content = { it ->
//            Box(modifier = Modifier.background(containerColor)) {
                Box(modifier = Modifier
                    .padding(it)
//                    .background(containerColor)
                ) {
                    content(it)
                }
//            }
        },
    )
}
