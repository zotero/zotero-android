package org.zotero.android.uicomponents

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.UriHandler

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

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
        context.startActivity(intent)
    }
}
