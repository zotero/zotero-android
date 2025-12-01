package org.zotero.android.screens.dashboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import org.greenrobot.eventbus.EventBus
import org.zotero.android.BuildConfig
import org.zotero.android.androidx.content.longToast
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint.AllItems
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.navigation.DashboardTopLevelDialogs
import org.zotero.android.architecture.navigation.phone.DashboardRootPhoneNavigation
import org.zotero.android.architecture.navigation.tablet.DashboardRootTopLevelTabletNavigation
import org.zotero.android.architecture.navigation.toolbar.SyncToolbarScreen
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.files.FileStore
import org.zotero.android.ktx.enableEdgeToEdgeAndTranslucency
import org.zotero.android.uicomponents.themem3.AppThemeM3
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
internal class DashboardActivity : BaseActivity() {

    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>

    private val viewModel: DashboardViewModel by viewModels()

    private var pickFileCallPoint: CallPoint = AllItems

    @Inject
    lateinit var defaults: Defaults

    @Inject
    lateinit var fileStore: FileStore

    @Inject
    lateinit var dispatchers: Dispatchers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeAndTranslucency()

        KeyboardVisibilityEvent.setEventListener(
            this,
            this,
            listener = object : KeyboardVisibilityEventListener {
                override fun onVisibilityChanged(isOpen: Boolean) {
                    EventBus.getDefault().post(EventBusConstants.OnKeyboardVisibilityChange(isOpen))
                }

            }
        )

        pickFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                EventBus.getDefault()
                    .post(EventBusConstants.FileWasSelected(uri, pickFileCallPoint))
            }
        }

        val onPickFile: (callPoint: CallPoint) -> Unit = { callPoint ->
            pickFileCallPoint = callPoint
            pickFileLauncher.launch(pickFileIntent())
        }
        val onOpenFile: (file: File, mimeType: String) -> Unit = { file, mimeType ->
            val fileProviderAuthority = BuildConfig.APPLICATION_ID + ".provider"
            val resultUri = FileProvider.getUriForFile(this, fileProviderAuthority, file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(resultUri, mimeType)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, resultUri)
            intent.flags = FLAG_GRANT_READ_URI_PERMISSION
            showAppChooserExcludingZoteroApp(intent)
        }

        val onOpenWebpage: (url: String) -> Unit = { url ->
            var prefixedUrl = url
            if (!prefixedUrl.startsWith("http://") && !prefixedUrl.startsWith("https://")) {
                prefixedUrl = "http://$prefixedUrl"
            }


            val intent = Intent(Intent.ACTION_VIEW, prefixedUrl.toUri())
            //Some devices have no apps to open URLs or such function was restricted.
            try {
                startActivity(intent)
            } catch (e: Exception) {
                val errorMessage =
                    "No app found to open web pages or restriction is in place. URL = $prefixedUrl"
                Timber.e(e, errorMessage)
                longToast(errorMessage)
            }
        }

        val onExportPdf: (file: File) -> Unit = { file ->
            val fileProviderAuthority = BuildConfig.APPLICATION_ID + ".provider"
            val resultUri = FileProvider.getUriForFile(this, fileProviderAuthority, file)
            val share = Intent()
            share.setAction(Intent.ACTION_SEND)
            share.setDataAndType(resultUri, "application/pdf")
            share.putExtra(Intent.EXTRA_STREAM, resultUri)
            showAppChooserExcludingZoteroApp(share)
        }

        val onExportHtml: (file: File) -> Unit = { file ->
            val fileProviderAuthority = BuildConfig.APPLICATION_ID + ".provider"
            val resultUri = FileProvider.getUriForFile(this, fileProviderAuthority, file)
            val share = Intent()
            share.setAction(Intent.ACTION_SEND)
            share.setDataAndType(resultUri, "text/html")
            share.putExtra(Intent.EXTRA_STREAM, resultUri)
            showAppChooserExcludingZoteroApp(share)
        }

        val mainCoroutineScope = CoroutineScope(dispatchers.main)
        mainCoroutineScope.launch {
            val wasPspdfkitInitialized = defaults.wasPspdfkitInitialized()
            val collectionDefaultValue = viewModel.getInitialCollectionArgs()

            setContent {
                AppThemeM3 {
                    Box {
                        val viewState by viewModel.viewStates.observeAsState(DashboardViewState())
                        val viewEffect by viewModel.viewEffects.observeAsState()
                        val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()
                        LaunchedEffect(key1 = viewModel) {
                            viewModel.init(isTablet = isTablet)
                        }
                        if (viewState.initialLoadData != null) {
                            val layoutType = CustomLayoutSize.calculateLayoutType()
                            if (layoutType.isTablet()) {
                                DashboardRootTopLevelTabletNavigation(
                                    collectionDefaultValue = collectionDefaultValue,
                                    onPickFile = onPickFile,
                                    viewEffect = viewEffect,
                                    onOpenFile = onOpenFile,
                                    onOpenWebpage = onOpenWebpage,
                                    wasPspdfkitInitialized = wasPspdfkitInitialized,
                                    onExportPdf = onExportPdf,
                                    onExportHtml = onExportHtml,
                                    onExitApp = { finish() }
                                )
                            } else {
                                DashboardRootPhoneNavigation(
                                    collectionDefaultValue = collectionDefaultValue,
                                    onPickFile = onPickFile,
                                    onOpenFile = onOpenFile,
                                    onOpenWebpage = onOpenWebpage,
                                    wasPspdfkitInitialized = wasPspdfkitInitialized,
                                    viewEffect = viewEffect,
                                    onExportPdf = onExportPdf,
                                    onExportHtml = onExportHtml,
                                    onExitApp = { finish() }
                                )
                            }
                            DashboardTopLevelDialogs(viewState = viewState, viewModel = viewModel)
                            SyncToolbarScreen()
                        }
                    }
                }
            }
        }

    }

    private fun showAppChooserExcludingZoteroApp(intent: Intent) {
        val noAppFoundMessage = "No app found to open this file"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val chooserIntent = Intent.createChooser(intent, "Share file")
            val allIntentActivities = packageManager.queryIntentActivities(intent, 0)
            val excludedApps = allIntentActivities
                .filter { it.activityInfo.name.contains("org.zotero.android") }
                .map {
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                }
            chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedApps.toTypedArray())
            if (allIntentActivities.size == excludedApps.size) {
                longToast(noAppFoundMessage)
            } else {
                startActivity(chooserIntent)
            }
        } else {
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                longToast(noAppFoundMessage)
            }
        }
    }

    private fun pickFileIntent(): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        return Intent.createChooser(intent, "Pick File")
    }

    companion object {
        fun getIntentClearTask(
            context: Context,
        ): Intent {
            return Intent(context, DashboardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }

        fun getIntent(
            context: Context,
        ): Intent {
            return Intent(context, DashboardActivity::class.java).apply {
            }
        }
    }
}

