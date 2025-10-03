package org.zotero.android.screens.itemdetails

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.helpers.formatter.dateFormatItemDetails
import org.zotero.android.screens.itemdetails.rows.ItemDetailsFieldRow
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import java.util.Date


@Composable
fun AddItemRow(
    titleRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Drawables.add_circle_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .weight(1f),
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DatesRows(
    dateAdded: Date,
    dateModified: Date,
    isEditMode: Boolean,
) {
    if (isEditMode) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemDetailsFieldRow(
                detailTitle = stringResource(id = Strings.date_added),
                detailValue = dateFormatItemDetails().format(dateAdded),
                onRowTapped = {
                    //no action on tap, but still show ripple effect
                }
            )
        }
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemDetailsFieldRow(
                stringResource(id = Strings.date_modified),
                dateFormatItemDetails().format(dateModified),
                onRowTapped = {
                    //no action on tap, but still show ripple effect
                }
            )
        }
    } else {
        ItemDetailsFieldRow(
            detailTitle = stringResource(id = Strings.date_added),
            detailValue = dateFormatItemDetails().format(dateAdded),
            onRowTapped = {
                //no action on tap, but still show ripple effect
            }
        )
        ItemDetailsFieldRow(
            stringResource(id = Strings.date_modified),
            dateFormatItemDetails().format(dateModified),
            onRowTapped = {
                //no action on tap, but still show ripple effect
            }
        )
    }

}

@Composable
internal fun ItemDetailHeaderSection(
    sectionTitle: Int,
) {
    Column(modifier = Modifier.height(28.dp), verticalArrangement = Arrangement.Center) {
        Text(
            modifier = Modifier,
            text = stringResource(id = sectionTitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}


