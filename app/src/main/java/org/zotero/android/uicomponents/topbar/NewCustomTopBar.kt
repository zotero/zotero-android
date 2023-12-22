package org.zotero.android.uicomponents.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension.Companion.fillToConstraints
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun NewCustomTopBar(
    title: String? = null,
    leftContainerContent: List<@Composable (RowScope.() -> Unit)> = emptyList(),
    rightContainerContent: List<@Composable (RowScope.() -> Unit)> = emptyList(),
    shouldFillMaxWidth: Boolean = true,
    shouldAddBottomDivider: Boolean = true,
    backgroundColor: Color = CustomTheme.colors.topBarBackgroundColor,
    leftGuidelineStartPercentage: Float = 0.3f,
    rightGuidelineStartPercentage: Float = 0.3f,
) {
    var modifier: Modifier = Modifier
    if (shouldFillMaxWidth) {
        modifier = modifier.fillMaxWidth()
    }
    ConstraintLayout(
        modifier = modifier
            .height(56.dp)
            .background(color = backgroundColor)
    ) {
        val (leftContainer, rightContainer, titleContainer, bottomDivider) = createRefs()

        if (leftContainerContent.isNotEmpty()) {
            Row(modifier = Modifier.constrainAs(leftContainer) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
            }) {
                leftContainerContent.forEach {
                    Spacer(modifier = Modifier.width(8.dp))
                    it()
                }
            }
        }

        if (title != null) {
            val startGuideline = createGuidelineFromStart(leftGuidelineStartPercentage)
            val endGuideline = createGuidelineFromEnd(rightGuidelineStartPercentage)
            Text(
                modifier = Modifier
//                    .background(color = Color.Red)
                    .constrainAs(titleContainer) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(startGuideline)
                        end.linkTo(endGuideline)
                        width = fillToConstraints
                    },
                text = title,
                style = CustomTheme.typography.newH2,
                color = CustomTheme.colors.defaultTextColor,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (rightContainerContent.isNotEmpty()) {
            Row(modifier = Modifier.constrainAs(rightContainer) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }) {
                rightContainerContent.forEach {
                    it()
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
        if (shouldAddBottomDivider) {
            NewDivider(
                modifier = Modifier
                    .constrainAs(bottomDivider) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
            )
        }

    }
}

@Composable
fun NewCustomTopBarWithTitleContainer(
    titleContainerContent: (@Composable (Modifier) -> Unit)? = null,
    leftContainerContent: List<@Composable (RowScope.() -> Unit)> = emptyList(),
    rightContainerContent: List<@Composable (RowScope.() -> Unit)> = emptyList(),
    shouldFillMaxWidth: Boolean = true,
    shouldAddBottomDivider: Boolean = true,
    backgroundColor: Color = CustomTheme.colors.topBarBackgroundColor
) {
    var modifier: Modifier = Modifier
    if (shouldFillMaxWidth) {
        modifier = modifier.fillMaxWidth()
    }
    ConstraintLayout(
        modifier = modifier
            .height(56.dp)
            .background(color = backgroundColor)
    ) {
        val (leftContainer, rightContainer, titleContainer, bottomDivider) = createRefs()

        if (leftContainerContent.isNotEmpty()) {
            Row(modifier = Modifier.constrainAs(leftContainer) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
            }) {
                leftContainerContent.forEach {
                    Spacer(modifier = Modifier.width(8.dp))
                    it()
                }
            }
        }

        if (titleContainerContent != null) {
            val startGuideline = createGuidelineFromStart(0.3f)
            val endGuideline = createGuidelineFromEnd(0.3f)
            titleContainerContent(
                Modifier
//                    .background(color = Color.Red)
                    .constrainAs(titleContainer) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(startGuideline)
                        end.linkTo(endGuideline)
                        width = fillToConstraints
                    },
            )
        }

        if (rightContainerContent.isNotEmpty()) {
            Row(modifier = Modifier.constrainAs(rightContainer) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }) {
                rightContainerContent.forEach {
                    it()
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
        if (shouldAddBottomDivider) {
            NewDivider(
                modifier = Modifier
                    .constrainAs(bottomDivider) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
            )
        }

    }
}