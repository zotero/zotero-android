package org.zotero.android.pdf

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import org.zotero.android.R
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.databinding.PdfReaderAnnotationHighlightBinding

class PdfAnnotationsListRecyclerAdapter(
    private val activity: Activity,
    private val viewModel: PdfReaderViewModel,
    private val itemClickListener: (AnnotationKey) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HIGHLIGHT = 0
    private val TYPE_NOTE = 1
    private val TYPE_IMAGE = 2
    private val TYPE_INK = 3

    private val imageRequestManager: RequestManager = Glide.with(activity)

    private val inflater: LayoutInflater = LayoutInflater.from(activity)

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
                val itemBinding = PdfReaderAnnotationHighlightBinding.inflate(inflater, parent, false)
                return InkHolder(itemBinding)
            }

            TYPE_IMAGE -> {
                val itemBinding = PdfReaderAnnotationHighlightBinding.inflate(inflater, parent, false)
                return ImageHolder(itemBinding)
            }

            TYPE_NOTE -> {
                val itemBinding = PdfReaderAnnotationHighlightBinding.inflate(inflater, parent, false)
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
        when (holder) {
            is ImageHolder -> {
                holder.bind(position)
            }

            is InkHolder -> {
                holder.bind(position)
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

    inner class InkHolder(private val itemBinding: PdfReaderAnnotationHighlightBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(index: Int) {
//            val key = viewModel.viewState.sortedKeys[index]
//            itemBinding.mainText.text = "Ink "+  key.key
        }
    }

    inner class HighlightHolder(private val itemBinding: PdfReaderAnnotationHighlightBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(index: Int) {
            val key = viewModel.viewState.sortedKeys[index]
            val annotation = viewModel.annotation(key)!!
            val annotationColor = Color.parseColor(annotation.color)
            itemBinding.highlighterIcon.setColorFilter(annotationColor)
            itemBinding.headerTitle.text =
                activity.getString(R.string.page_number, annotation.pageLabel)
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

    inner class NoteHolder(private val itemBinding: PdfReaderAnnotationHighlightBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(index: Int) {

        }
    }

    inner class ImageHolder(private val itemBinding: PdfReaderAnnotationHighlightBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(index: Int) {
//            val key = viewModel.viewState.sortedKeys[index]
//            itemBinding.mainText.text = "Image "+  key.key
        }
    }

}