package org.zotero.android.pdf

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
import org.zotero.android.pdf.cache.AnnotationPreviewCacheUpdatedEventStream
import org.zotero.android.pdf.cache.AnnotationPreviewFileCache
import org.zotero.android.pdf.cache.AnnotationPreviewMemoryCache
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

    @Inject
    lateinit var annotationPreviewMemoryCache: AnnotationPreviewMemoryCache

    @Inject
    lateinit var annotationPreviewFileCache: AnnotationPreviewFileCache

    @Inject
    lateinit var annotationPreviewCacheUpdatedEventStream: AnnotationPreviewCacheUpdatedEventStream

    private lateinit var adapter: PdfAnnotationListRecyclerAdapter

    private val viewModel: PdfReaderViewModel by viewModels()

    private lateinit var fragment: PdfFragment
    private lateinit var configuration: PdfConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setupViews()
        setupRecyclerView()
        setupToolbar()
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

    private fun setupToolbar() {
        window.statusBarColor =
            ContextCompat.getColor(this, R.color.pdf_annotations_topbar_background)
        binding.showSideMenuButton.setOnClickListener {
            val isStart = binding.mainContainer.progress == 0.0f
            if (isStart) {
                binding.mainContainer.transitionToEnd()
            } else {
                binding.mainContainer.transitionToStart()
            }
        }

    }

    private fun setupViews() = with(binding) {
        setContentView(root)
    }

    override fun trigger(effect: PdfReaderViewEffect) = when (effect) {
        PdfReaderViewEffect.NavigateBack -> {

        }
        is PdfReaderViewEffect.UpdateAnnotationsList -> {
            adapter.update()
            if (effect.scrollToIndex != -1) {
                binding.recyclerView.smoothScrollToPosition(effect.scrollToIndex)
            } else {
            }
        }
        else -> {}
    }

    override fun render(state: PdfReaderViewState) = with(binding) {

    }

    override fun onDocumentLoaded(document: PdfDocument) {
        viewModel.init(
            document = document,
            fragment = fragment,
            annotationMaxSideSize = annotationMaxSideSize()
        )
    }

    private fun updatePdfConfiguration() {
        val pdfSettings = fileStore.getPDFSettings()

        configuration = PdfConfiguration.Builder()
            .scrollDirection(pdfSettings.direction)
            .scrollMode(pdfSettings.transition)
            .fitMode(pdfSettings.pageFitting)
            .layoutMode(pdfSettings.pageMode)
            .themeMode(pdfSettings.appearanceMode)
            .disableFormEditing()
            .disableAnnotationRotation()
            .setSelectedAnnotationResizeEnabled(false)
            .build()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PdfAnnotationListRecyclerAdapter(
            activity = this,
            viewModel = viewModel,
            annotationPreviewMemoryCache = this.annotationPreviewMemoryCache,
            annotationPreviewFileCache = this.annotationPreviewFileCache,
            annotationPreviewCacheUpdatedEventStream = this.annotationPreviewCacheUpdatedEventStream
        ) { selectedAnnotation ->
            viewModel.selectAnnotation(selectedAnnotation)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun annotationMaxSideSize(): Int {
        val outValue = TypedValue()
        resources.getValue(R.dimen.pdf_sidebar_width_percent, outValue, true)
        val sidebarWidthPercentage = outValue.float
        val annotationSize = Resources.getSystem().displayMetrics.widthPixels * sidebarWidthPercentage
        return annotationSize.toInt()
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

