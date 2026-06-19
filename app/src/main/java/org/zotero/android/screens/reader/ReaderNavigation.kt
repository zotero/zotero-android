
package org.zotero.android.screens.reader

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogFixedDimens
import org.zotero.android.screens.reader.annotation.ReaderAnnotationNavigation
import org.zotero.android.screens.reader.annotation.toReaderAnnotationScreen
import org.zotero.android.screens.reader.annotationmore.ReaderAnnotationMoreNavigation
import org.zotero.android.screens.reader.annotationmore.toReaderAnnotationMoreScreen
import org.zotero.android.screens.reader.colorpicker.ReaderColorPickerScreen
import org.zotero.android.screens.reader.filter.ReaderFilterNavigation
import org.zotero.android.screens.reader.filter.toReaderFilterScreen
import org.zotero.android.screens.reader.settings.ReaderSettingsScreen

internal const val ARG_READER_SCREEN = "readerReaderArgs"
internal const val ARG_READER_SETTINGS_SCREEN = "readerSettingsArgs"

internal fun NavGraphBuilder.readerNavScreensForTablet(
    navigateToTagPicker: () -> Unit,
    onOpenWebpage: (url: String) -> Unit,
    navigation: ZoteroNavigation,
    navController: NavHostController,
) {
    readerScreen(
        onBack = navigation::onBack,
        navigateToPdfFilter = navigation::toReaderFilterNavigation,
        navigateToTagPicker = navigateToTagPicker,
        navigateToReaderAnnotation = navigation::toReaderAnnotationNavigation,
        navigateToReaderAnnotationMore = navigation::toReaderAnnotationMoreNavigation,
        navigateToReaderColorPicker = navigation::toReaderColorPicker,
        navigateToReaderSettings = navigation::toReaderSettings,
        onOpenWebpage = onOpenWebpage,

    )
    dialogFixedDimens(
        modifier = Modifier
            .height(400.dp)
            .width(400.dp),
        route = ReaderDestinations.READER_FILTER_NAVIGATION,
    ) {
        ReaderFilterNavigation(
            args = ScreenArguments.readerFilterArgs,
            onBack = navigation::onBack
        )
    }

    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = "${ReaderDestinations.READER_SETTINGS}/{$ARG_READER_SETTINGS_SCREEN}",
        arguments = listOf(
            navArgument(ARG_READER_SETTINGS_SCREEN) { type = NavType.StringType },
        ),
    ) {
        ReaderSettingsScreen(args = null, onBack = navController::popBackStack)
    }

    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = ReaderDestinations.READER_ANNOTATION_MORE_NAVIGATION,
    ) {
        ReaderAnnotationMoreNavigation(
            args = ScreenArguments.readerAnnotationMoreArgs,
            onBack = navigation::onBack
        )
    }
    dialogFixedDimens(
        modifier = Modifier
            .height(220.dp)
            .width(300.dp),
        route = ReaderDestinations.READER_COLOR_PICKER,
    ) {
        ReaderColorPickerScreen(
            args = ScreenArguments.readerColorPickerArgs,
            onBack = navController::popBackStack,
        )
    }

    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = ReaderDestinations.READER_ANNOTATION_NAVIGATION,
    ) {
        ReaderAnnotationNavigation(
            args = ScreenArguments.readerAnnotationArgs,
            onBack = navigation::onBack
        )
    }
}

internal fun NavGraphBuilder.readerNavScreensForPhone(
    navigation: ZoteroNavigation,
    navigateToTagPicker: () -> Unit,
    onOpenWebpage: (url: String) -> Unit,
) {
    readerScreen(
        onBack = navigation::onBack,
        navigateToPdfFilter = navigation::toReaderFilterScreen,
        navigateToTagPicker = navigateToTagPicker,
        navigateToReaderAnnotation = navigation::toReaderAnnotationScreen,
        navigateToReaderAnnotationMore = navigation::toReaderAnnotationMoreScreen,
        navigateToReaderColorPicker = navigation::toReaderColorPicker,
        navigateToReaderSettings = navigation::toReaderSettings,
        onOpenWebpage = onOpenWebpage,
    )
}

private fun NavGraphBuilder.readerScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToReaderAnnotation: () -> Unit,
    navigateToReaderAnnotationMore: () -> Unit,
    navigateToReaderColorPicker: () -> Unit,
    navigateToReaderSettings: (args: String) -> Unit,
    onOpenWebpage: (url: String) -> Unit,
) {
    composable(
        route = "${ReaderDestinations.READER_SCREEN}/{$ARG_READER_SCREEN}",
        arguments = listOf(
            navArgument(ARG_READER_SCREEN) { type = NavType.StringType },
        ),
    ) {
        ReaderScreen(
            onBack = onBack,
            navigateToPdfFilter = navigateToPdfFilter,
            navigateToTagPicker = navigateToTagPicker,
            navigateToReaderAnnotation = navigateToReaderAnnotation,
            navigateToReaderAnnotationMore = navigateToReaderAnnotationMore,
            navigateToReaderColorPicker = navigateToReaderColorPicker,
            navigateToReaderSettings = navigateToReaderSettings,
            onOpenWebpage = onOpenWebpage
        )
    }
}

private object ReaderDestinations {
    const val READER_SCREEN = "readerScreen"
    const val READER_FILTER_NAVIGATION = "readerFilterNavigation"
    const val READER_ANNOTATION_MORE_NAVIGATION = "readerAnnotationMoreNavigation"
    const val READER_COLOR_PICKER = "readerColorPicker"
    const val READER_ANNOTATION_NAVIGATION = "readerAnnotationNavigation"
    const val READER_SETTINGS = "readerSettings"
}

fun ZoteroNavigation.toReaderScreen(
    readerParams: String,
) {
    navController.navigate("${ReaderDestinations.READER_SCREEN}/$readerParams")
}

private fun ZoteroNavigation.toReaderAnnotationNavigation() {
    navController.navigate(ReaderDestinations.READER_ANNOTATION_NAVIGATION)
}

private fun ZoteroNavigation.toReaderAnnotationMoreNavigation() {
    navController.navigate(ReaderDestinations.READER_ANNOTATION_MORE_NAVIGATION)
}

private fun ZoteroNavigation.toReaderColorPicker() {
    navController.navigate(ReaderDestinations.READER_COLOR_PICKER)
}

private fun ZoteroNavigation.toReaderFilterNavigation() {
    navController.navigate(ReaderDestinations.READER_FILTER_NAVIGATION)
}

private fun ZoteroNavigation.toReaderSettings(args: String) {
    navController.navigate("${ReaderDestinations.READER_SETTINGS}/$args")
}
