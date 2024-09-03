package org.zotero.android.screens.dashboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import dagger.hilt.android.AndroidEntryPoint
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
import org.zotero.android.architecture.navigation.phone.DashboardRootPhoneNavigation
import org.zotero.android.architecture.navigation.tablet.DashboardRootTopLevelTabletNavigation
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.theme.CustomTheme
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
internal class DashboardActivity : BaseActivity() {

    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>

    private val viewModel: DashboardViewModel by viewModels()

    private var pickFileCallPoint: CallPoint = AllItems

    @Inject
    lateinit var defaults: Defaults

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KeyboardVisibilityEvent.setEventListener(
            this,
            this,
            listener = object: KeyboardVisibilityEventListener {
                override fun onVisibilityChanged(isOpen: Boolean) {
                    EventBus.getDefault().post(EventBusConstants.OnKeyboardVisibilityChange(isOpen))
                }

            }
        )

        pickFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                EventBus.getDefault().post(EventBusConstants.FileWasSelected(uri, pickFileCallPoint))
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

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                longToast("No app found to open this file")
            }
        }

        val onOpenWebpage: (uri: Uri) -> Unit = { uri ->
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        val wasPspdfkitInitialized = defaults.wasPspdfkitInitialized()

        setContent {
            CustomTheme {
                val layoutType = CustomLayoutSize.calculateLayoutType()
                if (layoutType.isTablet()) {
                    DashboardRootTopLevelTabletNavigation(
                        onPickFile = onPickFile,
                        viewModel = viewModel,
                        onOpenFile = onOpenFile,
                        onOpenWebpage = onOpenWebpage,
                        wasPspdfkitInitialized = wasPspdfkitInitialized,
                    )
                } else {
                    DashboardRootPhoneNavigation(
                        onPickFile = onPickFile,
                        viewModel = viewModel,
                        onOpenFile = onOpenFile,
                        onOpenWebpage = onOpenWebpage,
                        wasPspdfkitInitialized = wasPspdfkitInitialized,
                    )
                }

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

