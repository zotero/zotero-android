package org.zotero.android.screens.citation.singlecitation

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.screens.settings.SettingsSectionTitle
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.controls.CustomSwitch
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SingleCitationScreen(
    onBack: () -> Unit,
    scaffoldModifier: Modifier = Modifier,
    viewModel: SingleCitationViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground
    CustomThemeWithStatusAndNavBars(
        navBarBackgroundColor = backgroundColor
    ) {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(SingleCitationViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is SingleCitationViewEffect.OnBack -> {
                    onBack()
                }

                else -> {
                    //no-op
                }
            }
        }
        CustomScaffold(
            modifier = scaffoldModifier,
            backgroundColor = CustomTheme.colors.popupBackgroundContent,
            topBar = {
                SingleCitationTopBar(
                    onCancelClicked = onBack,
                    onCopyTapped = viewModel::onCopyTapped,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                SettingsSection {
                    SettingsCitationDropBoxAndEditFieldItem(viewState, viewModel)
                    SettingsDivider()
                    SettingsCitationSwitchItem( title = stringResource(Strings.citation_omit_author),
                        isChecked = viewState.omitAuthor,
                        onCheckedChange = viewModel::onOmitAuthor)
                }
                Spacer(modifier = Modifier.height(30.dp))
                SettingsSectionTitle(titleId = Strings.citation_preview)
                SettingsSection {
                    SettingsCitationWebViewItem(
                        previewHtml = viewState.preview,
                        height = viewState.previewHeight
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.DropDownMenuBox(
    viewState: SingleCitationViewState,
    viewModel: SingleCitationViewModel
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    ExposedDropdownMenuBox(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 16.dp)
            .padding(vertical = 4.dp)
            .width(140.dp)
            .clip(RoundedCornerShape(10.dp))
            .requiredSizeIn(maxHeight = 50.dp),
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            value = localized(viewState.locator),
            onValueChange = { },
            textStyle = CustomTheme.typography.default,
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            locatorsList.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = localized(item),
                            color = CustomTheme.colors.defaultTextColor,
                            style = CustomTheme.typography.default,
                        )
                    },
                    onClick = {
                        viewModel.setLocator(item)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun localized(locator: String): String {
    val context = LocalContext.current
    val resourceId =
        context.resources.getIdentifier(
            "citation.locator.${locator.replace(' ', '_')}",
            "string",
            context.packageName
        )
    val stringResource = stringResource(resourceId)
    return stringResource
}

@Composable
private fun SettingsCitationDropBoxAndEditFieldItem(
    viewState: SingleCitationViewState,
    viewModel: SingleCitationViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
    ) {
        DropDownMenuBox(viewState, viewModel)

        CustomTextField(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 16.dp, start = 170.dp),
            value = viewState.locatorValue,
            hint = "Number",
            ignoreTabsAndCaretReturns = true,
            maxLines = 1,
            singleLine = true,
            onValueChange = viewModel::onLocatorValueChanged,
            textStyle = CustomTheme.typography.default,
            textColor = CustomTheme.colors.primaryContent,
        )
    }
}

@Composable
private fun SettingsCitationSwitchItem(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
    ) {
        androidx.compose.material.Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, end = 0.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.primaryContent,
        )
        CustomSwitch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
        )
    }
}

@Composable
private fun SettingsCitationWebViewItem(
    previewHtml: String,
    height: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = (height + 15).dp)
    ) {
        val color = Color(0xFFD1D1D6).toArgb()
        AndroidView(
            factory = { context ->
                val webView = WebView(context)
                webView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(web: WebView, url: String?) {
                        web.loadUrl("javascript:(function(){ " +
                                "document.body.style.paddingTop = '10px';" +
                                "document.body.style.paddingLeft = '10px';" +
                                "})();")
                    }
                }

                webView.setBackgroundColor(color)
                webView.settings.javaScriptEnabled = true
                webView.settings.allowFileAccess = true
                webView.settings.allowContentAccess = true
                webView.loadData(injectStyle(previewHtml), "text/html", "UTF-8")
                webView
            },
            update = { webView ->
                webView.loadData(injectStyle(previewHtml), "text/html", "UTF-8")
            }
        )
    }
}

private fun injectStyle(htmlString: String): String {
    val style = """
            <meta name="viewport" content="width=device-width">
            <style type="text/css">
                body{
                    font-size:1em;
                    font-family: -apple-system;
                    -webkit-text-size-adjust:100%;
                    color:black;
                    padding:0;
                    margin:0;
                    background-color: transparent;
                }

                @media (prefers-color-scheme: dark) {
                    body {
                        background-color:transparent;
                        color: white;
                    }
                }
            </style>
            """
    val headIndexStart = htmlString.indexOf("<head>")
    val htmlIndexStart = htmlString.indexOf("<html>")
    if (headIndexStart != -1) {
        val newStringBuilder = StringBuilder(htmlString)
        newStringBuilder.insert(headIndexStart + 6, style)
        return newStringBuilder.toString()
    } else if (htmlIndexStart != -1) {
        val newStringBuilder = StringBuilder(htmlString)
        newStringBuilder.insert(htmlIndexStart + 6,"<head>${style}</head>")
        return newStringBuilder.toString()
    } else {
        return "<html><head>$style</head><body>$htmlString</body></html>"
    }
}
