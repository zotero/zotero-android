package org.zotero.android.pdf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.ui.PdfFragment
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.R
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.Screen
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.databinding.PdfReaderActivityBinding
import org.zotero.android.files.FileStore
import javax.inject.Inject

@AndroidEntryPoint
internal class PdfReaderActivity : BaseActivity(), DocumentListener, Screen<
        PdfReaderViewState,
        PdfReaderViewEffect> {

    private val binding: PdfReaderActivityBinding by lazy {
        PdfReaderActivityBinding.inflate(layoutInflater)
    }

    @Inject
    lateinit var fileStore: FileStore

    private val viewModel: PdfReaderViewModel by viewModels()

    private lateinit var fragment: PdfFragment
    private lateinit var configuration: PdfConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setupViews()
        viewModel.observeViewChanges(this)
        updatePdfConfiguration()
        val params = ScreenArguments.pdfReaderArgs
        fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as PdfFragment?
            ?: run {
                val newFragment = PdfFragment.newInstance(params.uri, this.configuration)
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newFragment)
                    .commit()
                return@run newFragment
            }

        fragment.apply {
            addDocumentListener(this@PdfReaderActivity)
        }
    }

    private fun setupViews() = with(binding) {
        setContentView(root)
    }

    override fun trigger(effect: PdfReaderViewEffect) = when (effect) {

        else -> {}
    }

    override fun render(state: PdfReaderViewState) = with(binding) {

    }

    override fun onDocumentLoaded(document: PdfDocument) {
        viewModel.init(document)
    }

    private fun updatePdfConfiguration() {
        val pdfSettings = fileStore.getPDFSettings()

        configuration = PdfConfiguration.Builder()
            .scrollDirection(pdfSettings.direction)
            .scrollMode(pdfSettings.transition)
            .fitMode(pdfSettings.pageFitting)
            .layoutMode(pdfSettings.pageMode)
            .themeMode(pdfSettings.appearanceMode)
            .build()
    }

    companion object {
        fun getIntent(
            context: Context,
        ): Intent {
            return Intent(context, PdfReaderActivity::class.java).apply {
            }
        }
    }
}

