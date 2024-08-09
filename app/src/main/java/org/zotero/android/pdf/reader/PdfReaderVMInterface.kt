package org.zotero.android.pdf.reader

import android.net.Uri
import androidx.fragment.app.FragmentManager
import com.pspdfkit.ui.PdfThumbnailBar
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import org.zotero.android.pdf.cache.AnnotationPreviewMemoryCache
import org.zotero.android.pdf.data.Annotation

interface PdfReaderVMInterface {

    var annotationMaxSideSize: Int
    val annotationPreviewMemoryCache: AnnotationPreviewMemoryCache
    val activeAnnotationTool: AnnotationTool?
    var toolColors: MutableMap<AnnotationTool, String>

    fun init(
        uri: Uri,
        annotationMaxSideSize: Int,
        containerId: Int,
        fragmentManager: FragmentManager,
        isTablet: Boolean,
    )

    fun onTagsClicked(annotation: Annotation)
    fun onSearch(text: String)
    fun onCommentFocusFieldChange(annotationKey: String)
    fun onCommentTextChange(annotationKey: String, comment: String)
    fun onMoreOptionsForItemClicked()
    fun annotation(key: AnnotationKey): Annotation?
    fun selectAnnotation(key: AnnotationKey)
    fun loadPreviews(keys: List<String>)
    fun showFilterPopup()
    fun toggle(tool: AnnotationTool)
    fun showToolOptions()
    fun canUndo(): Boolean
    fun canRedo(): Boolean
    fun onUndoClick()
    fun onRedoClick()
    fun onCloseClick()
}