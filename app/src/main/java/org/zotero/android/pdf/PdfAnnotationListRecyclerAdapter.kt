package org.zotero.android.pdf

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.R
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.databinding.PdfReaderAnnotationHighlightBinding
import org.zotero.android.databinding.PdfReaderAnnotationImageBinding
import org.zotero.android.databinding.PdfReaderAnnotationInkBinding
import org.zotero.android.databinding.PdfReaderAnnotationNoteBinding
import org.zotero.android.pdf.cache.AnnotationPreviewCacheUpdatedEventStream
import org.zotero.android.pdf.cache.AnnotationPreviewFileCache
import org.zotero.android.pdf.cache.AnnotationPreviewMemoryCache

class PdfAnnotationListRecyclerAdapter(
    private val activity: Activity,
    private val viewModel: PdfReaderViewModel,
    private val annotationPreviewMemoryCache: AnnotationPreviewMemoryCache,
    private val annotationPreviewFileCache: AnnotationPreviewFileCache,
    private val annotationPreviewCacheUpdatedEventStream: AnnotationPreviewCacheUpdatedEventStream,
    private val itemClickListener: (AnnotationKey) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HIGHLIGHT = 0
    private val TYPE_NOTE = 1
    private val TYPE_IMAGE = 2
    private val TYPE_INK = 3

    private val inflater: LayoutInflater = LayoutInflater.from(activity)

    init {
        annotationPreviewCacheUpdatedEventStream.flow()
            .onEach { key ->
                notifyItemChanged(viewModel.viewState.sortedKeys.indexOfFirst { it.key == key })
            }
            .launchIn(viewModel.viewModelScope)
    }

    fun update() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_HIGHLIGHT -> {
                val itemBinding = PdfReaderAnnotationHighlightBinding.inflate(inflater, parent, false)
                return HighlightHolder(itemBinding)
            }

            TYPE_INK -> {
                val itemBinding = PdfReaderAnnotationInkBinding.inflate(inflater, parent, false)
                return InkHolder(itemBinding)
            }

            TYPE_IMAGE -> {
                val itemBinding = PdfReaderAnnotationImageBinding.inflate(inflater, parent, false)
                return ImageHolder(itemBinding)
            }

            TYPE_NOTE -> {
                val itemBinding = PdfReaderAnnotationNoteBinding.inflate(inflater, parent, false)
                return NoteHolder(itemBinding)
            }
        }
        throw RuntimeException("there is no type that matches the type $viewType + make sure your using types correctly")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val key = viewModel.viewState.sortedKeys[position]
        val annotation = viewModel.annotation(key)!!
        val selected = annotation.key == viewModel.viewState.selectedAnnotationKey?.key
        holder.itemView.setBackgroundResource(if (selected) {
            R.drawable.pdf_annotation_selected_item_background
        } else {
            R.drawable.pdf_annotation_item_background
        }
        )
        holder.itemView.setOnClickListener {
            itemClickListener(key)
        }
        val loadPreview = {
            val preview = annotationPreviewMemoryCache.getBitmap(annotation.key)
            if (preview == null) {
                loadPreviews(listOf(annotation.key))
            }
            preview
        }
        when (holder) {
            is ImageHolder -> {
                holder.bind(position, loadPreview)
            }

            is InkHolder -> {
                holder.bind(position, loadPreview)
            }

            is HighlightHolder -> {
                holder.bind(position)
            }

            is NoteHolder -> {
                holder.bind(position)
            }
        }
    }


    override fun getItemCount(): Int {
        return viewModel.viewState.sortedKeys.size
    }

    override fun getItemViewType(position: Int): Int {
        val annotation = viewModel.annotation(viewModel.viewState.sortedKeys[position]) ?: return -1

        return when (annotation.type) {
            AnnotationType.note -> TYPE_NOTE
            AnnotationType.highlight -> TYPE_HIGHLIGHT
            AnnotationType.image -> TYPE_IMAGE
            AnnotationType.ink -> TYPE_INK
        }
    }

    inner class InkHolder(private val itemBinding: PdfReaderAnnotationInkBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(index: Int, loadPreview: () -> Bitmap?) {
            val key = viewModel.viewState.sortedKeys[index]
            val annotation = viewModel.annotation(key)!!
            val color = annotation.displayColor
            val annotationColor = Color.parseColor(color)
            itemBinding.highlighterIcon.setColorFilter(annotationColor)
            itemBinding.headerTitle.text =
                activity.getString(R.string.page) + " " + annotation.pageLabel

            itemBinding.tagsCommentDivider.isVisible = !annotation.tags.isEmpty()
            itemBinding.tags.isVisible = !annotation.tags.isEmpty()
            itemBinding.tags.text = annotation.tags.joinToString(separator = ", ") { it.name }

            val bitmap = loadPreview()
            if (bitmap == null) {
                itemBinding.annotationImage.setImageResource(0)
            } else {
                itemBinding.annotationImage.setImageBitmap(bitmap)
                itemBinding.annotationImage.maxHeight = viewModel.annotationMaxSideSize
            }
        }
    }

    inner class ImageHolder(private val itemBinding: PdfReaderAnnotationImageBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(index: Int, loadPreview: () -> Bitmap?) {
            val key = viewModel.viewState.sortedKeys[index]
            val annotation = viewModel.annotation(key)!!
            val annotationColor = Color.parseColor(annotation.displayColor)
            itemBinding.highlighterIcon.setColorFilter(annotationColor)
            itemBinding.headerTitle.text =
                activity.getString(R.string.page) + " " + annotation.pageLabel

            itemBinding.tagsCommentDivider.isVisible =
                !annotation.tags.isEmpty() && !annotation.comment.isEmpty()
            itemBinding.tags.isVisible = !annotation.tags.isEmpty()
            itemBinding.comment.isVisible = !annotation.comment.isEmpty()

            itemBinding.comment.text = annotation.comment
            itemBinding.tags.text = annotation.tags.joinToString(separator = ", ") { it.name }

            val bitmap = loadPreview()
            if (bitmap == null) {
                itemBinding.annotationImage.setImageResource(0)
            } else {
                itemBinding.annotationImage.setImageBitmap(bitmap)
                itemBinding.annotationImage.maxHeight = viewModel.annotationMaxSideSize
            }
        }
    }


    inner class HighlightHolder(private val itemBinding: PdfReaderAnnotationHighlightBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(index: Int) {
            val key = viewModel.viewState.sortedKeys[index]
            val annotation = viewModel.annotation(key)!!
            val annotationColor = Color.parseColor(annotation.displayColor)
            itemBinding.highlighterIcon.setColorFilter(annotationColor)
            itemBinding.headerTitle.text =
                activity.getString(R.string.page) + " " + annotation.pageLabel
            itemBinding.bodyHighlight.setBackgroundColor(annotationColor)
            itemBinding.body.text = annotation.text ?: ""

            itemBinding.tagsCommentDivider.isVisible =
                !annotation.tags.isEmpty() && !annotation.comment.isEmpty()
            itemBinding.tags.isVisible = !annotation.tags.isEmpty()
            itemBinding.comment.isVisible = !annotation.comment.isEmpty()

            itemBinding.comment.text = annotation.comment
            itemBinding.tags.text = annotation.tags.joinToString(separator = ", ") { it.name }
        }
    }

    inner class NoteHolder(private val itemBinding: PdfReaderAnnotationNoteBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(index: Int) {
            val key = viewModel.viewState.sortedKeys[index]
            val annotation = viewModel.annotation(key)!!
            val annotationColor = Color.parseColor(annotation.displayColor)
            itemBinding.highlighterIcon.setColorFilter(annotationColor)
            itemBinding.headerTitle.text =
                activity.getString(R.string.page) + " " + annotation.pageLabel

            val showTagsAndCommentsLayout = !annotation.tags.isEmpty() || !annotation.comment.isEmpty()
            itemBinding.headerBodyDivider.isVisible =
                showTagsAndCommentsLayout
            itemBinding.tagsCommentsLayout.isVisible =
                showTagsAndCommentsLayout

            itemBinding.tagsCommentDivider.isVisible =
                !annotation.tags.isEmpty() && !annotation.comment.isEmpty()
            itemBinding.tags.isVisible = !annotation.tags.isEmpty()
            itemBinding.comment.isVisible = !annotation.comment.isEmpty()

            itemBinding.comment.text = annotation.comment
            itemBinding.tags.text = annotation.tags.joinToString(separator = ", ") { it.name }
        }
    }

    private fun loadPreviews(keys: List<String>) {
        if (keys.isEmpty()) else { return }

        val isDark = viewModel.viewState.isDark
        val libraryId = viewModel.viewState.library.identifier

        for (key in keys) {
            if (annotationPreviewMemoryCache.getBitmap(key) != null) {
                continue
            }
            annotationPreviewFileCache.preview(key = key, parentKey = viewModel.viewState.key, libraryId = libraryId, isDark = isDark)
        }
    }

}