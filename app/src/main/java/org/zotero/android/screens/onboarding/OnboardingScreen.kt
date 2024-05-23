package org.zotero.android.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.androidx.text.StyledTextHelper
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.button.PrimaryButton
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

private val onboardingPages =
    listOf(
        OnboardingPage(Strings.onboarding_access, Drawables.onboarding_access),
        OnboardingPage(Strings.onboarding_annotate, Drawables.onboarding_annotate),
        OnboardingPage(Strings.onboarding_share, Drawables.onboarding_share),
        OnboardingPage(Strings.onboarding_sync, Drawables.onboarding_sync),
    )

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun OnboardingScreen(
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
) {
    CustomThemeWithStatusAndNavBars {
        val layoutType = CustomLayoutSize.calculateLayoutType()
//    SolidStatusBar()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {

            Column(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
            ) {
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
                        Text(
                            text = StyledTextHelper.annotatedStringResource(
                                LocalContext.current,
                                stringRes = onboardingPage.strRes
                            ),
                            modifier = Modifier.height((26 * 3).dp),
                            color = CustomTheme.colors.primaryContent,
                            textAlign = TextAlign.Center,
                            style = CustomTheme.typography.default.copy(lineHeight = 26.sp),
                            fontSize = layoutType.calculateTextSize(),
                        )
                        Spacer(modifier = Modifier.height(layoutType.calculatePadding()))
                        Image(
                            painter = painterResource(onboardingPage.drawableRes),
                            contentDescription = null,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DotsIndicator(
                        totalDots = pagerState.pageCount,
                        selectedIndex = pagerState.currentPage,
                        selectedColor = CustomPalette.CoolGray,
                        unSelectedColor = CustomPalette.Charcoal
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = Strings.onboarding_sign_in),
                        onClick = onSignInClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = Strings.onboarding_create_account),
                        onClick = {
                            uriHandler.openUri("https://www.zotero.org/user/register?app=1")
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        modifier = Modifier
                            .safeClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    uriHandler.openUri("https://www.zotero.org/?app=1")
                                }
                            ),
                        text = stringResource(id = Strings.about_zotero),
                        color = CustomTheme.colors.zoteroBlueWithDarkMode,
                        style = CustomTheme.typography.default,
                        fontSize = layoutType.calculateTextSize(),
                    )
                }
                Spacer(modifier = Modifier.weight(0.7f))
            }
        }

    }


}

@Composable
fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    selectedColor: Color,
    unSelectedColor: Color,
) {
    LazyRow(
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()

    ) {

        items(totalDots) { index ->
            if (index == selectedIndex) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(unSelectedColor)
                )
            }

            if (index != totalDots - 1) {
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}
