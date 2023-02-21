package org.zotero.android.architecture

import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.creatoredit.data.CreatorEditArgs
import org.zotero.android.screens.itemdetails.data.ShowItemDetailsArgs
import org.zotero.android.screens.mediaviewer.image.ImageViewerArgs
import org.zotero.android.screens.mediaviewer.video.VideoPlayerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.uicomponents.singlepicker.SinglePickerArgs

object ScreenArguments {
    lateinit var showItemDetailsArgs: ShowItemDetailsArgs
    lateinit var addOrEditNoteArgs: AddOrEditNoteArgs
    lateinit var creatorEditArgs: CreatorEditArgs
    lateinit var tagPickerArgs: TagPickerArgs
    lateinit var singlePickerArgs: SinglePickerArgs
    lateinit var videoPlayerArgs: VideoPlayerArgs
    lateinit var imageViewerArgs: ImageViewerArgs
}
