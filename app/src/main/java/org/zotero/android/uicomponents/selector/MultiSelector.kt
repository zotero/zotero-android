package org.zotero.android.uicomponents.selector

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.uicomponents.theme.CustomTheme
import kotlin.math.absoluteValue

private const val AnimationDurationMillis = 500

@Stable
interface MultiSelectorState {
    val selectedIndex: Float
    val startCornerPercent: Int
    val endCornerPercent: Int
    val textColors: List<Color>

    fun selectOption(scope: CoroutineScope, index: Int)
}

@Stable
class MultiSelectorStateImpl(
    options: List<MultiSelectorOption>,
    selectedOption: Int,
    private val selectedColor: Color,
    private val unselectedColor: Color,
) : MultiSelectorState {

    override val selectedIndex: Float
        get() = _selectedIndex.value
    override val startCornerPercent: Int
        get() = _startCornerPercent.value.toInt()
    override val endCornerPercent: Int
        get() = _endCornerPercent.value.toInt()

    override val textColors: List<Color>
        get() = _textColors.value

    private var _selectedIndex = Animatable(options.indexOfFirst {it.id == selectedOption}.toFloat())
    private var _startCornerPercent = Animatable(
        if (options.first().id == selectedOption) {
            50f
        } else {
            15f
        }
    )
    private var _endCornerPercent = Animatable(
        if (options.last().id == selectedOption) {
            50f
        } else {
            15f
        }
    )

    private var _textColors: State<List<Color>> = derivedStateOf {
        List(numOptions) { index ->
            lerp(
                start = unselectedColor,
                stop = selectedColor,
                fraction = 1f - (((selectedIndex - index.toFloat()).absoluteValue).coerceAtMost(1f))
            )
        }
    }

    private val numOptions = options.size
    private val animationSpec = tween<Float>(
        durationMillis = AnimationDurationMillis,
        easing = FastOutSlowInEasing,
    )

    override fun selectOption(scope: CoroutineScope, index: Int) {
        scope.launch {
            _selectedIndex.animateTo(
                targetValue = index.toFloat(),
                animationSpec = animationSpec,
            )
        }
        scope.launch {
            _startCornerPercent.animateTo(
                targetValue = if (index == 0) 50f else 15f,
                animationSpec = animationSpec,
            )
        }
        scope.launch {
            _endCornerPercent.animateTo(
                targetValue = if (index == numOptions - 1) 50f else 15f,
                animationSpec = animationSpec,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiSelectorStateImpl

        if (selectedColor != other.selectedColor) return false
        if (unselectedColor != other.unselectedColor) return false
        if (_selectedIndex != other._selectedIndex) return false
        if (_startCornerPercent != other._startCornerPercent) return false
        if (_endCornerPercent != other._endCornerPercent) return false
        if (numOptions != other.numOptions) return false
        if (animationSpec != other.animationSpec) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedColor.hashCode()
        result = 31 * result + unselectedColor.hashCode()
        result = 31 * result + _selectedIndex.hashCode()
        result = 31 * result + _startCornerPercent.hashCode()
        result = 31 * result + _endCornerPercent.hashCode()
        result = 31 * result + numOptions
        result = 31 * result + animationSpec.hashCode()
        return result
    }
}

@Composable
fun rememberMultiSelectorState(
    options: List<MultiSelectorOption>,
    selectedOptionId: Int,
    selectedColor: Color,
    unSelectedColor: Color,
) = remember {
    MultiSelectorStateImpl(
        options,
        selectedOptionId,
        selectedColor,
        unSelectedColor,
    )
}

enum class MultiSelectorDrawOption {
    Option,
    Background,
}

@Composable
fun MultiSelector(
    options: List<MultiSelectorOption>,
    selectedOptionId: Int,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = CustomTheme.colors.primaryContent,
    unselectedcolor: Color = CustomTheme.colors.primaryContent,
    state: MultiSelectorState = rememberMultiSelectorState(
        options = options,
        selectedOptionId = selectedOptionId,
        selectedColor = selectedColor,
        unSelectedColor = unselectedcolor,
    ),
    fontSize: TextUnit,
) {
    require(options.size >= 2) { "This composable requires at least 2 options" }
    require(options.any { it.id == selectedOptionId}) { "Invalid selected option [$selectedOptionId]" }
    LaunchedEffect(key1 = options, key2 = selectedOptionId) {
        state.selectOption(this, options.indexOfFirst { it.id == selectedOptionId})
    }
    Layout(
        modifier = modifier
            .clip(
                shape = RoundedCornerShape(percent = 50)
            )
            .background(CustomTheme.colors.sortPickerUnSelected),
        content = {
            val colors = state.textColors
            options.forEachIndexed { index, option ->
                Box(
                    modifier = Modifier
                        .layoutId(MultiSelectorDrawOption.Option)
                        .clickable { onOptionSelect(option.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.optionString,
                        style = CustomTheme.typography.default,
                        color = selectedColor,
                        fontSize = fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .layoutId(MultiSelectorDrawOption.Background)
                    .padding(2.dp)
                    .clip(
                        shape = RoundedCornerShape(
                            topStartPercent = state.startCornerPercent,
                            bottomStartPercent = state.startCornerPercent,
                            topEndPercent = state.endCornerPercent,
                            bottomEndPercent = state.endCornerPercent,
                        )
                    )
                    .background(CustomTheme.colors.sortPickerSelected),
            )
        }
    ) { measurables, constraints ->
        val optionWidth = constraints.maxWidth / options.size
        val optionConstraints = Constraints.fixed(
            width = optionWidth,
            height = constraints.maxHeight,
        )
        val optionPlaceables = measurables
            .filter { measurable -> measurable.layoutId == MultiSelectorDrawOption.Option }
            .map { measurable -> measurable.measure(optionConstraints) }
        val backgroundPlaceable = measurables
            .first { measurable -> measurable.layoutId == MultiSelectorDrawOption.Background }
            .measure(optionConstraints)
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        ) {
            backgroundPlaceable.placeRelative(
                x = (state.selectedIndex * optionWidth).toInt(),
                y = 0,
            )
            optionPlaceables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = optionWidth * index,
                    y = 0,
                )
            }
        }
    }
}

@Preview(widthDp = 420)
@Composable
fun PreviewMultiSelector() {
    CustomTheme {
        Surface(
            color = CustomTheme.colors.surface,
        ) {
            val options1 = listOf(
                MultiSelectorOption(1, "Lorem"),
                MultiSelectorOption(2, "Ipsum"),
                MultiSelectorOption(3, "Dolor")
            )
            var selectedOption1 by remember {
                mutableStateOf(options1.first().id)
            }
            val options2 =
                listOf(
                    MultiSelectorOption(1, "Sit"),
                    MultiSelectorOption(2, "Amet"),
                    MultiSelectorOption(3, "Consectetur"),
                    MultiSelectorOption(4, "Elit"),
                    MultiSelectorOption(5, "Quis")
                )

            var selectedOption2 by remember {
                mutableStateOf(options2.first().id)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MultiSelector(
                    options = options1,
                    selectedOptionId = selectedOption1,
                    onOptionSelect = { option ->
                        selectedOption1 = option
                    },
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    fontSize = CustomTheme.typography.default.fontSize
                )

                MultiSelector(
                    options = options2,
                    selectedOptionId = selectedOption2,
                    onOptionSelect = { option ->
                        selectedOption2 = option
                    },
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    fontSize = CustomTheme.typography.default.fontSize
                )
            }
        }
    }
}