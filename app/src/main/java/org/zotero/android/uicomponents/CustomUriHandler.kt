package org.zotero.android.uicomponents

import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri

class CustomUriHandler(private val context: Context) : UriHandler {

    override fun openUri(uri: String) {
        val formattedUrl = if (
            !uri.startsWith("http://") &&
            !uri.startsWith("https://")
        ) {
            "http://$uri"
        } else {
            uri
        }

        val intent = Intent(Intent.ACTION_VIEW, formattedUrl.toUri())
        context.startActivity(intent)
    }
}
