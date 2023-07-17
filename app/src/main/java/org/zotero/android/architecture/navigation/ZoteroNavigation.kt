package org.zotero.android.architecture.navigation

import androidx.activity.OnBackPressedDispatcher
import androidx.navigation.NavHostController

class ZoteroNavigation(
    val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()
}