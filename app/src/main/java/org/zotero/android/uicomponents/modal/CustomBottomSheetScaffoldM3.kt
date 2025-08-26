package org.zotero.android.uicomponents.modal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.snackbar.CustomSnackbarHost
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CustomBottomSheetScaffoldM3(
    sheetContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    topBar: (@Composable () -> Unit)? = null,
    snackbarMessage: SnackbarMessage? = null,
    sheetPeekHeight: Dp = 0.dp,
    content: @Composable () -> Unit = {}
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    BottomSheetScaffold(
        sheetContent = {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)

            ) {
                Box(
                ) {
                    sheetContent()
                }
            }
        },
        modifier = modifier,
        scaffoldState = scaffoldState,
        topBar = topBar,
        containerColor = Color.Transparent,
        snackbarHost = {
            CustomSnackbarHost(
                state = scaffoldState.snackbarHostState,
                snackbarMessage = snackbarMessage
            )
        },
        sheetMaxWidth = if (layoutType.isTablet()) {
            300.dp
        } else {
            BottomSheetDefaults.SheetMaxWidth
        },
        sheetPeekHeight = sheetPeekHeight,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetDragHandle = null,
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                Scrim(
                    bottomSheetState = scaffoldState.bottomSheetState,
                )
            }
        }
    )
}

@Composable
private fun Scrim(
    bottomSheetState: SheetState,
) {

    val coroutineScope = rememberCoroutineScope()

    // This extra box serves as an alpha container, we can't set alpha to
    // the color directly because scrim already has alpha built in
    Box(modifier = Modifier) {
        Spacer(
            modifier = Modifier
                .background(CustomTheme.colors.scrim)
                .fillMaxSize()
                .debounceClickable(
                    onClick = {
                        coroutineScope.launch {
                            bottomSheetState.partialExpand()
                        }
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        )
    }
}

@Composable
fun CustomModalBottomSheetM3(
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit,
    shouldCollapse: Boolean = false,
    snackbarMessage: SnackbarMessage? = null,
    sheetContent: @Composable BoxScope.() -> Unit,
) {
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,

    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    // Run just once on first composition
    LaunchedEffect(Unit) {
        bottomSheetState.expand()
    }

    LaunchedEffect(shouldCollapse) {
        if (shouldCollapse) bottomSheetState.partialExpand()
    }

    val coroutineScope = rememberCoroutineScope()
    BackHandler(onBack = {
        coroutineScope.launch {
            bottomSheetState.partialExpand()
        }
    })

    if (bottomSheetState.currentValue != SheetValue.PartiallyExpanded) {
        DisposableEffect(Unit) {
            onDispose {
                /*
                We need an additional check here because onDispose will also fire
                when the composable leaves the composition (i.e. navigate to
                another screen without collapsing the bottom sheet). In this case
                we don't want to fire onCollapse.
                 */
                if (bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                    onCollapse()
                }
            }
        }
    }

    CustomBottomSheetScaffoldM3(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetContent = { sheetContent() },
        snackbarMessage = snackbarMessage,
        content = { /* No content */ },
    )
}
