package org.zotero.android.pdffilter

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun PdfFilterNavigation(
) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        PdfFilterNavigation(navController, dispatcher)
    }

    val layoutType = CustomLayoutSize.calculateLayoutType()
    ZoteroNavHost(
        navController = navController,
        startDestination = PdfFilterDestinations.PDF_FILTER_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        pdfFilterScreen(onBack = navigation::onBack, navigateToTagPicker = navigation::toTagPicker)
        tagPickerScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.pdfFilterScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = PdfFilterDestinations.PDF_FILTER_SCREEN,
        arguments = listOf(),
    ) {
        PdfFilterScreen(onBack = onBack, navigateToTagPicker = navigateToTagPicker)
    }
}

private fun NavGraphBuilder.tagPickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = PdfFilterDestinations.TAG_PICKER_SCREEN,
        arguments = listOf(),
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private object PdfFilterDestinations {
    const val PDF_FILTER_SCREEN = "pdfFilterScreen"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
}

@SuppressWarnings("UseDataClass")
private class PdfFilterNavigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toTagPicker() {
        navController.navigate(PdfFilterDestinations.TAG_PICKER_SCREEN)
    }
}
