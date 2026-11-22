package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.downloads.DownloadStartResult
import com.example.downloads.enqueueFileDownload
import java.net.URLEncoder

private data class QuickPortal(val label: String, val url: String)

private val quickPortals = listOf(
    QuickPortal("DuckDuckGo", "https://duckduckgo.com"),
    QuickPortal("World Radio", "https://radio.garden"),
    QuickPortal("Marwadi Folk", "https://archive.org/details/audio?query=rajasthani+folk"),
    QuickPortal("Photopea Editor", "https://www.photopea.com"),
    QuickPortal("F-Droid Apps", "https://f-droid.org"),
    QuickPortal("Wikipedia", "https://www.wikipedia.org")
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivateBrowserScreen(
    requestedUrl: String?,
    onConsumeRequestedUrl: () -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenDownloads: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var addressInput by remember { mutableStateOf(requestedUrl ?: "https://duckduckgo.com") }
    var currentUrl by remember { mutableStateOf(requestedUrl ?: "https://duckduckgo.com") }
    var pageProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(requestedUrl) {
        if (!requestedUrl.isNullOrBlank()) {
            val normalized = normalizeUrlOrSearch(requestedUrl)
            addressInput = normalized
            currentUrl = normalized
            webViewRef?.loadUrl(normalized)
            onConsumeRequestedUrl()
        }
    }

    fun navigateTo(raw: String) {
        val target = normalizeUrlOrSearch(raw)
        addressInput = target
        currentUrl = target
        webViewRef?.loadUrl(target)
        focusManager.clearFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Private Browser Header & Controls
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Private Mode",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Private Browser",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
                    }
                    IconButton(
                        onClick = onOpenDownloads,
                        modifier = Modifier.testTag("browser_downloads_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "Open Downloads")
                    }
                    IconButton(
                        onClick = {
                            webViewRef?.apply {
                                clearHistory()
                                clearCache(true)
                                clearFormData()
                            }
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                            WebStorage.getInstance().deleteAllData()
                            navigateTo("https://duckduckgo.com")
                            onShowMessage("Private session wiped: cookies, cache & history cleared!")
                        },
                        modifier = Modifier.testTag("clear_private_browser_button")
                    ) {
                        Icon(
                            Icons.Rounded.CleaningServices,
                            contentDescription = "Clear private browsing data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    placeholder = { Text("Enter any website URL or search...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { navigateTo(addressInput) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("browser_url_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPortals.forEach { portal ->
                        AssistChip(
                            onClick = { navigateTo(portal.url) },
                            label = { Text(portal.label) }
                        )
                    }
                }
            }
        }

        if (pageProgress in 1..99) {
            LinearProgressIndicator(
                progress = { pageProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        settings.saveFormData = false
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            when (val result = enqueueFileDownload(
                                context = context,
                                url = url,
                                title = "Play Hub Download",
                                userAgent = userAgent,
                                contentDisposition = contentDisposition,
                                mimeType = mimetype
                            )) {
                                is DownloadStartResult.Started -> {
                                    onShowMessage("Downloading ${result.fileName}...")
                                }
                                is DownloadStartResult.Failed -> {
                                    onShowMessage(result.message)
                                }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                if (!url.isNullOrBlank()) {
                                    addressInput = url
                                    currentUrl = url
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress
                            }
                        }
                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun normalizeUrlOrSearch(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "https://duckduckgo.com"
    if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) return trimmed
    if (!trimmed.contains(" ") && trimmed.contains(".")) {
        return "https://$trimmed"
    }
    val encoded = URLEncoder.encode(trimmed, "UTF-8")
    return "https://duckduckgo.com/?q=$encoded"
}
