package org.zotero.android.screens.addnote

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.androidx.content.getDrawableByItemType
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.uicomponents.Drawables

@Composable
internal fun AddNoteTopBar(
    titleData: AddOrEditNoteArgs.TitleData?,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val type = titleData?.type
                if (type != null) {
                    Image(
                        painter = painterResource(
                            id = LocalContext.current.getDrawableByItemType(
                                ItemTypes.iconName(
                                    type,
                                    null
                                )
                            )
                        ),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                val title = titleData?.title
                if (title != null) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }


        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(Drawables.arrow_back_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )

}