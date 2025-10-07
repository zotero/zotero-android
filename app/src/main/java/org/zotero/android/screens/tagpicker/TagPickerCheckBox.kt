package org.zotero.android.screens.tagpicker

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable

@Composable
internal fun TagPickerCheckBox(
    isChecked: Boolean,
) {
    Checkbox(checked = isChecked, onCheckedChange = {})
}