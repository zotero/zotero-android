package org.zotero.android.uicomponents.modal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.FabPosition
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.snackbar.CustomSnackbarHost
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CustomBottomSheetScaffold(
    sheetContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    topBar: (@Composable () -> Unit)? = null,
    snackbarMessage: SnackbarMessage? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    sheetPeekHeight: Dp = 0.dp,
    backgroundColor: Color = CustomTheme.colors.surface,
    contentColor: Color = CustomTheme.colors.primaryContent,
    content: @Composable () -> Unit = {}
) {
    BottomSheetScaffold(
        sheetContent = {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)

            ) {
                DraggableIndicator()
                Box(
                    modifier = Modifier
                        .background(
                            color = CustomTheme.colors.surface,
                            shape = CustomTheme.shapes.bottomSheet,
                        )
                        .clip(CustomTheme.shapes.bottomSheet)
                ) {
                    sheetContent()
                }
            }
        },
        modifier = modifier,
        scaffoldState = scaffoldState,
        topBar = topBar,
        snackbarHost = {
            CustomSnackbarHost(
                state = scaffoldState.snackbarHostState,
                snackbarMessage = snackbarMessage
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        sheetElevation = 0.dp,
        // Because of drag indicator the shape is handled inside sheetContent
        sheetBackgroundColor = Color.Transparent,
        sheetContentColor = CustomTheme.colors.primaryContent,
        sheetPeekHeight = sheetPeekHeight,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
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
private fun ColumnScope.DraggableIndicator() {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .width(40.dp)
            .height(4.dp)
            .background(
                color = CustomTheme.colors.surface.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp),
            )
            .align(Alignment.CenterHorizontally)
    )
}

@Composable
private fun Scrim(
    bottomSheetState: BottomSheetState,
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
                            bottomSheetState.collapse()
                        }
                    },
                    indication = null,
                    interactionSource = MutableInteractionSource()
                )
        )
    }
}

@Composable
fun CustomModalBottomSheet(
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit,
    shouldCollapse: Boolean = false,
    snackbarMessage: SnackbarMessage? = null,
    sheetContent: @Composable BoxScope.() -> Unit,
) {
    val bottomSheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    // Run just once on first composition
    LaunchedEffect(Unit) {
        bottomSheetState.expand()
    }

    LaunchedEffect(shouldCollapse) {
        if (shouldCollapse) bottomSheetState.collapse()
    }

    val coroutineScope = rememberCoroutineScope()
    BackHandler(onBack = {
        coroutineScope.launch {
            bottomSheetState.collapse()
        }
    })

    if (bottomSheetState.currentValue != BottomSheetValue.Collapsed) {
        DisposableEffect(Unit) {
            onDispose {
                /*
                We need an additional check here because onDispose will also fire
                when the composable leaves the composition (i.e. navigate to
                another screen without collapsing the bottom sheet). In this case
                we don't want to fire onCollapse.
                 */
                if (bottomSheetState.currentValue == BottomSheetValue.Collapsed) {
                    onCollapse()
                }
            }
        }
    }

    CustomBottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetContent = { sheetContent() },
        backgroundColor = Color.Transparent,
        snackbarMessage = snackbarMessage,
        content = { /* No content */ }
    )
}
