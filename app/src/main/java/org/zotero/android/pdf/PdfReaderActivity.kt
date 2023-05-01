package org.zotero.android.pdf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.ui.PdfFragment
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.R
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.files.FileStore
import javax.inject.Inject

@AndroidEntryPoint
internal class PdfReaderActivity : BaseActivity(), DocumentListener {

    @Inject
    lateinit var defaults: Defaults

    @Inject
    lateinit var fileStore: FileStore

    private lateinit var fragment: PdfFragment
    private lateinit var configuration: PdfConfiguration
    private lateinit var pdfSettings: PDFSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.pdf_reader_activity)

        val params = ScreenArguments.pdfReaderArgs
        pdfSettings = fileStore.getPDFSettings()
        val username = defaults.getUsername()

        updatePdfConfiguration()

        fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as PdfFragment?
            ?: run {
                val newFragment = PdfFragment.newInstance(params.uri, configuration)
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

    private fun updatePdfConfiguration() {
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

