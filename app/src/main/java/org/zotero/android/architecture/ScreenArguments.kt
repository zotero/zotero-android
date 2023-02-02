package org.zotero.android.architecture

import org.zotero.android.dashboard.data.AddOrEditNoteArgs
import org.zotero.android.dashboard.data.CreatorEditArgs
import org.zotero.android.dashboard.data.ShowItemDetailsArgs
import org.zotero.android.dashboard.data.SinglePickerArgs

object ScreenArguments {
    lateinit var showItemDetailsArgs: ShowItemDetailsArgs
    lateinit var addOrEditNoteArgs: AddOrEditNoteArgs
    lateinit var creatorEditArgs: CreatorEditArgs
    lateinit var singlePickerArgs: SinglePickerArgs
}
