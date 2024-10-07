package org.zotero.android.screens.addbyidentifier

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.ktx.unmarshalLinkedHashMap
import org.zotero.android.ktx.unmarshalList
import org.zotero.android.translator.data.WebViewError
import org.zotero.android.translator.helper.TranslatorHelper
import timber.log.Timber
import kotlin.coroutines.resume

class LookupWebViewHandler constructor(
    dispatchers: Dispatchers,
    private val context: Context,
    private val gson: Gson,
    private val nonZoteroApi: NonZoteroApi,
) {
    private val uiMainCoroutineScope = CoroutineScope(dispatchers.main)

    private lateinit var webView: WebView
    private lateinit var webViewPort: WebMessagePort

    var cookies: String? = null
    var userAgent: String? = null
    var referrer: String? = null

    fun load(url: String, onWebViewLoadPage: () -> Unit, processWebViewResponses: ((message: WebMessage) -> Unit)? = null) {
        uiMainCoroutineScope.launch {
            webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowFileAccessFromFileURLs = true
            webView.settings.allowUniversalAccessFromFileURLs = true
            webView.settings.allowContentAccess = true
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d(
                        "WEBVIEW_LOG_TAG",
                        consoleMessage.message() + " -- From line "
                                + consoleMessage.lineNumber() + " of "
                                + consoleMessage.sourceId()
                    )
                    return super.onConsoleMessage(consoleMessage)
                }
            }
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    val channel = webView.createWebMessageChannel()
                    val port = channel[0]
                    this@LookupWebViewHandler.webViewPort = port
                    port.setWebMessageCallback(object :
                        WebMessagePort.WebMessageCallback() {
                        override fun onMessage(port: WebMessagePort, message: WebMessage) {
                            processWebViewResponses?.invoke(message)
                        }
                    })
                    //Passing to JS code the handle to our onMessage listener on kotlin side
                    webView.postWebMessage(
                        WebMessage("initPort", arrayOf(channel[1])),
                        Uri.EMPTY
                    )
                    onWebViewLoadPage()
                }
            }
            webView.loadUrl(url)
        }

    }

    fun evaluateJavascript(javascript: String, result: (String) -> Unit) {
        uiMainCoroutineScope.launch {
            webView.evaluateJavascript(javascript) { result ->
                result(result)
            }
        }
    }

    suspend fun sendMessaging(error: String, messageId: Long) {
        val payload = mapOf("error" to mapOf("message" to error))
        sendMessaging(payload,  messageId)
    }

    suspend fun sendMessaging(payload: Map<String, Any>, messageId: Long) {
        return suspendCancellableCoroutine { cont ->
            val encodedPayload = TranslatorHelper.encodeAsJSONForJavascript(gson = gson, data = payload)
            evaluateJavascript("javascript:Zotero.Messaging.receiveResponse('${messageId}', '${encodedPayload}')") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun sendRequest(options: JsonObject, messageId: Long) {
        val urlString = options["url"]?.asString
        val url = urlString
        val method = options["method"]?.asString
        if (url == null || method == null) {
            Timber.i("Incorrect URL request from javascript")
            Timber.i("$options")

            val data = "Incorrect URL request from javascript"
            sendHttpResponse(
                data = data,
                statusCode = -1,
                url = null,
                successCodes = listOf(200),
                headers = mapOf(),
                messageId = messageId
            )
            return
        }
        if(urlString.contains("repo/code/undefined")){
            Timber.e("WebViewHandler: Undefined call, translator missing.")
            throw WebViewError.urlMissingTranslators
        }

        val headers: MutableMap<String, String> =
            (options["headers"]?.unmarshalLinkedHashMap(gson) ?: mutableMapOf())
        val jsonBody = options["body"]
        val body: String? = if (jsonBody == null || jsonBody.isJsonNull) {
            null
        } else {
            jsonBody.asString
        }
        val successCodes: List<Int> = options["successCodes"]?.unmarshalList<Int>(gson) ?: emptyList()

        if (this.userAgent != null) {
            headers["User-Agent"] = this.userAgent!!
        }
        if (this.referrer != null) {
            headers["Referer"] = this.referrer!!
        }
        if (this.cookies != null) {
            headers["Cookie"] = this.cookies!!
        }
        val networkResult = safeApiCall {
            when (method) {
                "GET" -> {
                    nonZoteroApi.sendWebViewGet(
                        url = url,
                        headers = headers,
                    )
                }

                "POST" -> {
                    nonZoteroApi.sendWebViewPost(
                        url = url,
                        headers = headers,
                        textBody = body
                    )
                }

                else -> {
                    throw RuntimeException("Web Method not supported")
                }
            }

        }

        if (networkResult is CustomResult.GeneralSuccess.NetworkSuccess) {
            val statusCode = networkResult.httpCode
            val allHeaderFields = networkResult.headers.toMultimap()
            val data = networkResult.value!!.string()
            sendHttpResponse(
                data = data,
                statusCode = statusCode,
                url = url,
                successCodes = successCodes,
                headers = allHeaderFields,
                messageId = messageId
            )
        } else if (networkResult is CustomResult.GeneralError.NetworkError) {
            val error = networkResult.stringResponse
            sendHttpResponse(
                data = error,
                statusCode = networkResult.httpCode,
                url = null,
                successCodes = successCodes,
                headers = mapOf(),
                messageId = messageId
            )
        } else if (networkResult is CustomResult.GeneralError.CodeError) {
            val error = networkResult.throwable.localizedMessage
            sendHttpResponse(
                data = error,
                statusCode = -1,
                url = null,
                successCodes = successCodes,
                headers = mapOf(),
                messageId = messageId
            )
        }

    }

    suspend fun sendHttpResponse(
        data: String?,
        statusCode: Int,
        url: String?,
        successCodes: List<Int>,
        headers: Map<String, List<String>>,
        messageId: Long
    ) {
        val isSuccess = if (successCodes.isEmpty()) {
            (200..<300).contains(statusCode)
        } else {
            successCodes.contains(statusCode)
        }
        val responseText = data ?: ""

        val payload: Map<String, Any>
        if (isSuccess) {
            payload = mapOf("status" to statusCode, "responseText" to responseText, "headers" to headers, "url" to (url ?: ""))
        } else {
            payload = mapOf("error" to mapOf("status" to statusCode, "responseText" to responseText))
        }

        sendMessaging(payload = payload, messageId = messageId)
    }


}