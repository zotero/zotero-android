package org.zotero.android.uicomponents.loading

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.LCE2
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun FullScreenLceBox(
    lce: LCE2,
    modifier: Modifier = Modifier,
    errorTitle: String = "",
    errorDescription: String = stringResource(Strings.error_list_load_body),
    errorButtonText: String = stringResource(Strings.error_list_load_refresh),
    content: @Composable BoxScope.() -> Unit,
) {
    BaseLceBox(
        lce = lce,
        modifier = modifier,
        error = { lceError ->
            FullScreenError(
                modifier = Modifier.align(Center),
                errorTitle = errorTitle,
                errorDescription = errorDescription,
                errorButtonText = errorButtonText,
                errorAction = { lceError.tryAgain() }
            )
        },
        loading = {
            CircularLoading()
        },
        content = content
    )
}

@Composable
fun BoxScope.CircularLoading() {
    CircularProgressIndicator(
        color = CustomTheme.colors.zoteroDefaultBlue,
        modifier = Modifier
            .size(48.dp)
            .align(Center),
    )
}

@Composable
fun BaseLceBox(
    lce: LCE2,
    modifier: Modifier = Modifier,
    error: @Composable BoxScope.(LCE2.LoadError) -> Unit,
    loading: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        when (lce) {
            LCE2.Content -> content()
            LCE2.Loading -> loading()
            is LCE2.LoadError -> error(lce)
        }
    }
}

@Composable
fun FullScreenAnimatedLceBox(
    lce: LCE2,
    modifier: Modifier = Modifier,
    errorTitle: String = "",
    errorDescription: String = stringResource(Strings.error_list_load_body),
    errorButtonText: String = stringResource(Strings.error_list_load_refresh),
    content: @Composable BoxScope.() -> Unit,
) {
    BaseAnimatedLceBox(
        lce = lce,
        modifier = modifier,
        error = { lceError ->
            FullScreenError(
                modifier = Modifier.align(Center),
                errorTitle = errorTitle,
                errorDescription = errorDescription,
                errorButtonText = errorButtonText,
                errorAction = { lceError.tryAgain() }
            )
        },
        loading = {
            CircularLoading()
        },
        content = content
    )
}

@Composable
fun BaseAnimatedLceBox(
    lce: LCE2,
    modifier: Modifier = Modifier,
    error: @Composable BoxScope.(LCE2.LoadError) -> Unit,
    loading: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Crossfade(targetState = lce) { _lce ->
        Box(modifier) {
            when (_lce) {
                LCE2.Content -> content()
                LCE2.Loading -> loading()
                is LCE2.LoadError -> error(_lce)
            }
        }
    }
}
