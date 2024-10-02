package org.zotero.android.architecture

import org.zotero.android.pdf.annotation.data.PdfAnnotationArgs
import org.zotero.android.pdf.annotationmore.data.PdfAnnotationMoreArgs
import org.zotero.android.pdf.annotationmore.editpage.data.PdfAnnotationEditPageArgs
import org.zotero.android.pdf.colorpicker.data.PdfReaderColorPickerArgs
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchArgs
import org.zotero.android.pdf.settings.data.PdfSettingsArgs
import org.zotero.android.pdffilter.data.PdfFilterArgs
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.allitems.data.AllItemsArgs
import org.zotero.android.screens.collectionedit.data.CollectionEditArgs
import org.zotero.android.screens.collectionpicker.data.CollectionPickerArgs
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.creatoredit.data.CreatorEditArgs
import org.zotero.android.screens.filter.data.FilterArgs
import org.zotero.android.screens.itemdetails.data.ItemDetailsArgs
import org.zotero.android.screens.mediaviewer.image.ImageViewerArgs
import org.zotero.android.screens.mediaviewer.video.VideoPlayerArgs
import org.zotero.android.screens.share.sharecollectionpicker.data.ShareCollectionPickerArgs
import org.zotero.android.screens.sortpicker.data.SortPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.uicomponents.singlepicker.SinglePickerArgs

object ScreenArguments {
    lateinit var collectionsArgs: CollectionsArgs
    lateinit var collectionEditArgs: CollectionEditArgs
    lateinit var allItemsArgs: AllItemsArgs
    lateinit var itemDetailsArgs: ItemDetailsArgs
    lateinit var addOrEditNoteArgs: AddOrEditNoteArgs
    lateinit var creatorEditArgs: CreatorEditArgs
    lateinit var sortPickerArgs: SortPickerArgs
    lateinit var filterArgs: FilterArgs
    lateinit var tagPickerArgs: TagPickerArgs
    lateinit var singlePickerArgs: SinglePickerArgs
    lateinit var videoPlayerArgs: VideoPlayerArgs
    lateinit var imageViewerArgs: ImageViewerArgs
    lateinit var collectionPickerArgs: CollectionPickerArgs
    lateinit var pdfFilterArgs: PdfFilterArgs
    lateinit var pdfSettingsArgs: PdfSettingsArgs
    lateinit var pdfAnnotationArgs: PdfAnnotationArgs
    lateinit var pdfAnnotationMoreArgs: PdfAnnotationMoreArgs
    lateinit var pdfAnnotationEditPageArgs: PdfAnnotationEditPageArgs
    lateinit var pdfReaderColorPickerArgs: PdfReaderColorPickerArgs
    lateinit var shareCollectionPickerArgs: ShareCollectionPickerArgs
    lateinit var pdfReaderSearchArgs: PdfReaderSearchArgs
}
