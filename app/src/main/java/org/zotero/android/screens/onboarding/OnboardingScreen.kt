package org.zotero.android.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.zotero.android.androidx.content.pxToDp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

private val onboardingPages =
    listOf(
        OnboardingPage(
            taglineRes = Strings.onboarding_access_tagline,
            descriptionRes = Strings.onboarding_access_description,
            drawableRes = Drawables.onboarding_access
        ),
        OnboardingPage(
            taglineRes = Strings.onboarding_annotate_tagline,
            descriptionRes = Strings.onboarding_annotate_description,
            drawableRes = Drawables.onboarding_annotate
        ),
        OnboardingPage(
            taglineRes = Strings.onboarding_share_tagline,
            descriptionRes = Strings.onboarding_share_description,
            drawableRes = Drawables.onboarding_share
        ),
        OnboardingPage(
            taglineRes = Strings.onboarding_sync_tagline,
            descriptionRes = Strings.onboarding_sync_description,
            drawableRes = Drawables.onboarding_sync
        ),
    )

@Composable
internal fun OnboardingScreen(
    onSignInClick: () -> Unit,
) {
    val whiteColor = Color.White
    val blackColor = Color.Black
    val backgroundColor = if (isSystemInDarkTheme()) blackColor else whiteColor
    val layoutType = CustomLayoutSize.calculateLayoutType()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
//            .verticalScroll(rememberScrollState())

            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        var contentAreaModifier = Modifier.fillMaxHeight()
        if (layoutType.isTablet()) {
            contentAreaModifier =
                contentAreaModifier.width((LocalWindowInfo.current.containerSize.height / 2).pxToDp())
        }

        Column(contentAreaModifier) {
            val uriHandler = LocalUriHandler.current
            val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
            Spacer(modifier = Modifier.weight(1f))
            HorizontalPager(
                state = pagerState,
            ) { pageIndex ->
                val onboardingPage = onboardingPages[pageIndex]
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.0f),
                        painter = painterResource(onboardingPage.drawableRes),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.height(104.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(onboardingPage.taglineRes),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLargeEmphasized
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(onboardingPage.descriptionRes),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                }
            }
            Spacer(modifier = Modifier.weight(1f))
            OnboardingButtonSection(
                pagerState = pagerState,
                onSignInClick = onSignInClick,
                uriHandler = uriHandler
            )
        }

    }

}

