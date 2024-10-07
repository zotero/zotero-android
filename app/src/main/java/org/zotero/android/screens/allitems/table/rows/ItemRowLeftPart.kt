package org.zotero.android.screens.allitems.table.rows

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.androidx.content.getDrawableByItemType
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.checkbox.CircleCheckBox

@Composable
internal fun ItemRowLeftPart(
    layoutType: CustomLayoutSize.LayoutType,
    model: ItemCellModel,
    isItemSelected: (key: String) -> Boolean,
    isEditing: Boolean,
) {
    AnimatedContent(targetState = isEditing, label = "") { isEditing ->
        if (isEditing) {
            Row {
                Spacer(modifier = Modifier.width(16.dp))
                CircleCheckBox(
                    isChecked = isItemSelected(model.key),
                    layoutType = layoutType
                )
            }
        }
    }
    Spacer(modifier = Modifier.width(16.dp))
    Image(
        modifier = Modifier.size(28.dp),
        painter = painterResource(id = LocalContext.current.getDrawableByItemType(model.typeIconName)),
        contentDescription = null,
    )
}