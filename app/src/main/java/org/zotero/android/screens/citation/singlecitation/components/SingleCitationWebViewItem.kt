package org.zotero.android.screens.citation.singlecitation.components

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun SingleCitationWebViewItem(
    previewHtml: String,
    height: Int,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val webViewBackgroundColorArgb = backgroundColor.toArgb()
    val roundCornerShape = RoundedCornerShape(size = 10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = (height + 15).dp)
            .padding(horizontal = 16.dp)
            .background(shape = roundCornerShape, color = backgroundColor)
            .clip(roundCornerShape)
    ) {
        AndroidView(
            factory = { context ->
                val webView = WebView(context)
                webView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(web: WebView, url: String?) {
                        web.loadUrl(
                            "javascript:(function(){ " +
                                    "document.body.style.paddingTop = '10px';" +
                                    "document.body.style.paddingLeft = '10px';" +
                                    "})();"
                        )
                    }
                }

                webView.setBackgroundColor(webViewBackgroundColorArgb)
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
        newStringBuilder.insert(htmlIndexStart + 6, "<head>${style}</head>")
        return newStringBuilder.toString()
    } else {
        return "<html><head>$style</head><body>$htmlString</body></html>"
    }
}