package org.zotero.android.uicomponents.textinput

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.alignments.CenterOfFirstLine
import org.zotero.android.uicomponents.alignments.placeCenterOfFirstAndLastLine
import org.zotero.android.uicomponents.foundation.rememberFocusRequester
import org.zotero.android.uicomponents.theme.CustomTheme
import kotlin.math.roundToInt

/**
 * This is the standard minimalistic text field used across application.
 * It also shows an error if [errorText] is provided.
 *
 * To align your text field properly use text alignment inside the [textStyle] param.
 *
 * @param textStyle Style applied to both the text field and the hint
 */
@Composable
fun CustomTextField(
    value: TextFieldValue,
    hint: String,
    onValueChange: (TextFieldValue) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    maxCharacters: Int = Int.MAX_VALUE,
    errorText: String? = null,
    textColor: Color = CustomTheme.colors.primaryContent,
    hintColor: Color = Color.Unspecified,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val innerModifier by remember {
        derivedStateOf { modifier.copyFillModifiers() }
    }

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusRequester.requestFocus()
                    }
                )
            },
        horizontalAlignment = textStyle.textAlign.asHorizontalAlignment,
    ) {
        val showHint = value.text.isEmpty()
        BasicTextField(
            value = value,
            visualTransformation = visualTransformation,
            onValueChange = {
                val boundedText = it.text.take(maxCharacters)
                onValueChange(it.copy(text = boundedText))
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .then(innerModifier)
                .placeCenterOfFirstAndLastLine(
                    maxLines = if (singleLine) 1 else maxLines,
                ),
            enabled = enabled,
            textStyle = textStyle.copyWithAlignmentCorrection(
                color = textColor,
                isHintShowing = showHint,
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            cursorBrush = SolidColor(LocalContentColor.current),
            decorationBox = { innerTextField ->
                DecorationBox(
                    showHint = showHint,
                    hint = hint,
                    hintColor = hintColor,
                    textStyle = textStyle,
                    innerTextField = innerTextField
                )
            }
        )
        ErrorLabel(
            text = errorText,
        )
    }
}

/**
 * This is the standard minimalistic text field used across application.
 * It also shows an error if [errorText] is provided.
 *
 * To align your text field properly use text alignment inside the [textStyle] param.
 *
 * @param textStyle Style applied to both the text field and the hint
 */
@Composable
fun CustomTextField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onEnterOrTab: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorText: String? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    hintColor: Color = Color.Unspecified,
    hintTextStyle: TextStyle = CustomTheme.typography.default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxCharacters: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = false,
    ignoreTabsAndCaretReturns: Boolean = true,
    textColor: Color = CustomTheme.colors.primaryContent,
    textStyle: TextStyle = CustomTheme.typography.default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val innerModifier by remember {
        derivedStateOf { modifier.copyFillModifiers() }
    }

    var textFieldValueState by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    val textFieldValue = textFieldValueState.copy(text = value)

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusRequester.requestFocus()
                    }
                )
            },
        horizontalAlignment = textStyle.textAlign.asHorizontalAlignment,
    ) {
        val showHint = value.isEmpty()
        var hasFocus by remember { mutableStateOf(false) }
        BasicTextField(
            value = textFieldValue,
            visualTransformation = visualTransformation,
            onValueChange = {
                if (ignoreTabsAndCaretReturns && (it.text.contains("\n") || it.text.contains("\t"))) {
                    return@BasicTextField
                }
                val boundedText = it.text.take(maxCharacters)
                val newValue = it.copy(text = boundedText)
                textFieldValueState = newValue
                onValueChange(newValue.text)
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .then(innerModifier)
                .onFocusChanged {
                    hasFocus = it.hasFocus
                }
                .onKeyEvent {
                    if (
                        hasFocus && it.type == KeyEventType.KeyDown
                        && onEnterOrTab != null
                        && (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER || it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_TAB)
                    ) {
                        onEnterOrTab()
                        return@onKeyEvent true
                    }
                    false
                }
                .placeCenterOfFirstAndLastLine(
                    maxLines = if (singleLine) 1 else maxLines,
                ),
            enabled = enabled,
            textStyle = textStyle.copyWithAlignmentCorrection(
                color = textColor,
                isHintShowing = showHint,
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            cursorBrush = SolidColor(LocalContentColor.current),
            decorationBox = { innerTextField ->
                DecorationBox(
                    showHint = showHint,
                    hint = hint,
                    hintColor = hintColor,
                    textStyle = hintTextStyle,
                    innerTextField = innerTextField
                )
            }
        )
        ErrorLabel(
            text = errorText,
        )
    }
}

@Composable
private fun DecorationBox(
    showHint: Boolean,
    hint: String,
    hintColor: Color = Color.Unspecified,
    textStyle: TextStyle,
    innerTextField: @Composable () -> Unit
) {
    val measurePolicy = remember(showHint) { DecorationBoxMeasurePolicy() }

    Layout(
        content = {
            if (showHint) {
                Text(
                    text = hint,
                    color = hintColor.takeOrElse { CustomTheme.colors.secondaryContent },
                    style = textStyle,
                    modifier = Modifier
                        .placeCenterOfFirstAndLastLine(),
                    onTextLayout = { result ->
                        val rect = result.getCursorRect(0)
                        measurePolicy.hintLineStart = rect.left.roundToInt()
                    }
                )
            }
            innerTextField()
        },
        measurePolicy = measurePolicy,
    )
}

@Composable
private fun ErrorLabel(
    text: String?,
) {
    if (!text.isNullOrEmpty()) {
        Text(
            text = text,
            color = CustomTheme.colors.error,
            style = CustomTheme.typography.info,
            modifier = Modifier
                .padding(top = 8.dp)
        )
    }
}

// When the hint is shown, we align it to the start so the cursor doesn't appear in the middle
// of the input.
private fun TextStyle.copyWithAlignmentCorrection(
    color: Color,
    isHintShowing: Boolean,
) = copy(
    color = color,
    textAlign = if (isHintShowing) TextAlign.Start else textAlign,
)

private val TextAlign?.asHorizontalAlignment: Alignment.Horizontal
    get() = when (this) {
        TextAlign.Center -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }

@Stable
private class DecorationBoxMeasurePolicy(
    hintLineStart: Int = 0,
) : MeasurePolicy {

    var hintLineStart by mutableIntStateOf(hintLineStart)

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val (hintMeasurable, inputMeasurable) = if (measurables.size == 1) {
            null to measurables[0]
        } else {
            measurables[0] to measurables[1]
        }

        val hintPlaceable = hintMeasurable?.measure(constraints)
        // We want to override the built-in minimum text size to match the size of the hint
        // if the hint is visible otherwise there will be alignment issues. This is noticeable
        // when the width of the Hint is less than TextFieldDefaults.MinWidth.
        val inputPlaceable = inputMeasurable.measure(
            constraints.copy(
                maxWidth = hintPlaceable?.width ?: constraints.maxWidth
            )
        )

        val requiredWidth = Integer.max(hintPlaceable?.width ?: 0, inputPlaceable.width)
        val requiredHeight = Integer.max(hintPlaceable?.height ?: 0, inputPlaceable.height)

        val centerOfFirstLine = hintPlaceable?.get(CenterOfFirstLine)
            ?: inputPlaceable[CenterOfFirstLine]

        return layout(
            width = requiredWidth,
            height = requiredHeight,
            alignmentLines = mapOf(
                CenterOfFirstLine to centerOfFirstLine
            )
        ) {
            hintPlaceable?.place(0, 0)
            inputPlaceable.place(hintLineStart, 0)
        }
    }
}

// Generates a new Modifier based on the receiver containing all the fillMax* modifiers.
// This is used to make sure the TextField matches the fill behavior of the Column.
private fun Modifier.copyFillModifiers() = foldIn<Modifier>(Modifier) { acc, element ->
    when (element) {
        Modifier.fillMaxSize() -> acc.fillMaxSize()
        Modifier.fillMaxWidth() -> acc.fillMaxWidth()
        Modifier.fillMaxHeight() -> acc.fillMaxHeight()
        else -> acc
    }
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Suppress("StringLiteralDuplication")
private fun CustomTextFieldPreview(
    @PreviewParameter(CustomTextFieldPreviewStyleProvider::class) style: CustomTextFieldPreviewStyle
) {
    val showText: Boolean = style != CustomTextFieldPreviewStyle.Active
    val showError: Boolean = style == CustomTextFieldPreviewStyle.Error
    val focusRequesterFactory: @Composable () -> FocusRequester = when (style) {
        CustomTextFieldPreviewStyle.Typing -> {
            { rememberFocusRequester(requestOnLaunch = true) }
        }
        else -> { { rememberFocusRequester() } }
    }

    @Composable
    fun component(
        text: String,
        hint: String,
        textStyle: TextStyle
    ) {
        val (updatedText, updateText) = remember { mutableStateOf(if (showText) text else "") }
        CustomTextField(
            value = updatedText,
            hint = hint,
            onValueChange = updateText,
            focusRequester = focusRequesterFactory.invoke(),
            textStyle = textStyle,
            errorText = if (showError) "Error message goes here" else null,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = CustomTheme.colors.surface)
        )
    }

    CustomTheme {
        Column(
            verticalArrangement = Arrangement.Absolute.spacedBy(16.dp),
            modifier = Modifier
                .background(color = CustomTheme.colors.surface)
                .padding(16.dp)
        ) {
            Text(
                text = style.name,
                style = CustomTheme.typography.h1
            )
            component(
                text = "My answer",
                hint = "Your answer",
                textStyle = CustomTheme.typography.h1.copy(textAlign = TextAlign.Center),
            )
            component(
                text = "This is a description",
                hint = "Tap to add a description",
                textStyle = CustomTheme.typography.h2.copy(textAlign = TextAlign.Center),
            )
            component(
                text = "This is a description",
                hint = "Tap to add a description",
                textStyle = CustomTheme.typography.default.copy(textAlign = TextAlign.Center),
            )

            component(
                text = "My answer",
                hint = "Your answer",
                textStyle = CustomTheme.typography.h1,
            )
            component(
                text = "This is a description",
                hint = "Tap to add a description",
                textStyle = CustomTheme.typography.h2,
            )
            component(
                text = "This is a description",
                hint = "Tap to add a description",
                textStyle = CustomTheme.typography.default,
            )
            component(
                text = "This is a really long description ".repeat(5),
                hint = "This is a really long hint ".repeat(5),
                textStyle = CustomTheme.typography.default,
            )
        }
    }
}

internal enum class CustomTextFieldPreviewStyle {
    Active,
    Typing,
    Error,
}

internal class CustomTextFieldPreviewStyleProvider :
    CollectionPreviewParameterProvider<CustomTextFieldPreviewStyle>(
        collection = CustomTextFieldPreviewStyle.entries
    )

