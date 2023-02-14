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
import androidx.compose.animation.ExperimentalAnimationApi
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.uicomponents.theme.CustomTheme

@ExperimentalAnimationApi
@AndroidEntryPoint
internal class DashboardActivity : BaseActivity() {

    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                EventBus.getDefault().post(EventBusConstants.FileWasSelected(uri))
            }
        }

        val onPickFile: () -> Unit = {
            pickFileLauncher.launch(pickFileIntent())
        }
        val onOpenGeneralUri: (uri: Uri, mimeType: String) -> Unit = { uri, mimeType ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, mimeType)
            intent.flags = FLAG_GRANT_READ_URI_PERMISSION
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivity(intent)
        }

        val onOpenWebpage: (uri: Uri) -> Unit = { uri ->
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        setContent {
            CustomTheme {
                DashboardScreen(
                    onBack = { finish() },
                    onPickFile = onPickFile,
                    viewModel = viewModel,
                    onOpenGeneralUri = onOpenGeneralUri,
                    onOpenWebpage = onOpenWebpage,
                )
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

