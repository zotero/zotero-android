package org.zotero.android.uicomponents.foundation

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Wrapper around [Modifier.clickable] that will set clickable modifier only if [onClick] lambda
 * is not null. This way click events won't be consumed silently.
 *
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will
 * appear disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: (() -> Unit)? = null
) = if (onClick != null) {
    this.debounceClickable(
        enabled = enabled,
        onClick = onClick,
        role = role,
        interactionSource = null,
        indication = null,
    )
} else {
    this
}

/**
 * Wrapper around [Modifier.clickable] that will set clickable modifier only if [onClick] lambda
 * is not null. This way click events won't be consumed silently.
 *
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [PressInteraction.Press] when this clickable is pressed. Only the initial (first) press will be
 * recorded and dispatched with [MutableInteractionSource].
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [LocalIndication] will be used. Pass `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will
 * appear disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
@Composable
fun Modifier.safeClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: (() -> Unit)? = null
) = if (onClick != null) {
    this.debounceClickable(
        enabled = enabled,
        onClick = onClick,
        role = role,
        indication = indication,
        interactionSource = interactionSource
    )
} else {
    this
}
