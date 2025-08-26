package org.zotero.android.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.button.CustomFilledButton
import org.zotero.android.uicomponents.button.CustomOutlineButton
import org.zotero.android.uicomponents.button.CustomTextButton

@Composable
internal fun OnboardingButtonSection(
    pagerState: PagerState,
    onSignInClick: () -> Unit,
    uriHandler: UriHandler,
) {
    Column(modifier = Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingDotsIndicator(
            totalDots = pagerState.pageCount,
            selectedIndex = pagerState.currentPage,
            selectedColor = MaterialTheme.colorScheme.onSurface,
            unSelectedColor = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        CustomFilledButton(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .height(56.dp),
            text = stringResource(id = Strings.onboarding_sign_in),
            onClick = onSignInClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        CustomOutlineButton(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .height(56.dp),
            text = stringResource(id = Strings.onboarding_create_account), onClick = {
                uriHandler.openUri("https://www.zotero.org/user/register?app=1")
            })
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextButton(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            text = stringResource(id = Strings.about_zotero),
            onClick = {
                uriHandler.openUri("https://www.zotero.org/?app=1")
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
