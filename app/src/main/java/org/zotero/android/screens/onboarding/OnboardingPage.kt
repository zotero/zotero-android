package org.zotero.android.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnboardingPage(
    @StringRes val taglineRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val drawableRes: Int
)