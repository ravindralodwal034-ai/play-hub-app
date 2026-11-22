package com.example.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.model.StoreApp

const val EXTRA_LAUNCH_APP_URL = "com.example.playhub.EXTRA_LAUNCH_APP_URL"
const val EXTRA_LAUNCH_APP_TITLE = "com.example.playhub.EXTRA_LAUNCH_APP_TITLE"

private const val PREFS_DOWNLOADS = "play_hub_downloads"
private const val KEY_DOWNLOAD_IDS = "tracked_download_ids"

data class DownloadedItem(
    val id: Long,
    val title: String,
    val localUri: String?,
    val status: Int,
    val reason: Int,
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val mimeType: String?,
    val lastModified: Long
) {
    val progressPercent: Int
        get() = if (bytesTotal > 0L) ((bytesDownloaded * 100L) / bytesTotal).toInt().coerceIn(0, 100) else 0
}

sealed class DownloadStartResult {
    data class Started(val downloadId: Long, val fileName: String) : DownloadStartResult()
    data class Failed(val message: String) : DownloadStartResult()
}

private val DIRECT_FILE_EXTENSIONS = listOf(
    ".apk", ".xapk", ".apks", ".zip", ".rar", ".7z", ".tar", ".gz",
    ".pdf", ".mp3", ".wav", ".ogg", ".flac", ".mp4", ".webm",
    ".png", ".jpg", ".jpeg", ".svg", ".json", ".obb", ".epub"
)

fun isDirectDownloadUrl(url: String): Boolean {
    val lower = url.lowercase().substringBefore('?').substringBefore('#')
    return DIRECT_FILE_EXTENSIONS.any { lower.endsWith(it) } ||
        url.contains("download=", ignoreCase = true)
}

fun enqueueFileDownload(
    context: Context,
    url: String,
    title: String,
    userAgent: String? = null,
    contentDisposition: String? = null,
    mimeType: String? = null
): DownloadStartResult {
    val trimmedUrl = url.trim()
    if (!trimmedUrl.startsWith("https://") && !trimmedUrl.startsWith("http://")) {
        return DownloadStartResult.Failed("Only HTTP/HTTPS links can be queued by Android Download Manager.")
    }

    return try {
        val dm = context.getSystemService(DownloadManager::class.java)
            ?: return DownloadStartResult.Failed("Download Manager is unavailable on this device.")

        val guessed = URLUtil.guessFileName(trimmedUrl, contentDisposition, mimeType)
        val safeTitle = title.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().ifBlank { "PlayHub_Item" }
        val fileName = if (guessed.isBlank() || guessed == "downloadfile.bin") {
            "${safeTitle}.html"
        } else {
            guessed
        }

        val request = DownloadManager.Request(Uri.parse(trimmedUrl)).apply {
            setTitle(title.ifBlank { fileName })
            setDescription("Downloaded via Play Hub")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            if (!mimeType.isNullOrBlank()) {
                setMimeType(mimeType)
            }
            if (!userAgent.isNullOrBlank()) {
                addRequestHeader("User-Agent", userAgent)
            }
            val cookies = runCatching { CookieManager.getInstance().getCookie(trimmedUrl) }.getOrNull()
            if (!cookies.isNullOrBlank()) {
                addRequestHeader("Cookie", cookies)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            } else {
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            }
        }

        val downloadId = dm.enqueue(request)
        saveTrackedDownloadId(context, downloadId)
        DownloadStartResult.Started(downloadId, fileName)
    } catch (e: Exception) {
        DownloadStartResult.Failed(e.localizedMessage ?: "Could not start download.")
    }
}

fun queryPlayHubDownloads(context: Context): List<DownloadedItem> {
    val ids = getTrackedDownloadIds(context)
    if (ids.isEmpty()) return emptyList()

    val dm = context.getSystemService(DownloadManager::class.java) ?: return emptyList()
    val query = DownloadManager.Query().setFilterById(*ids.toLongArray())
    val items = mutableListOf<DownloadedItem>()

    try {
        dm.query(query)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
            val dlIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val mimeIdx = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
            val modIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)

            while (cursor.moveToNext()) {
                items.add(
                    DownloadedItem(
                        id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L,
                        title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else "Download",
                        localUri = if (uriIdx >= 0) cursor.getString(uriIdx) else null,
                        status = if (statusIdx >= 0) cursor.getInt(statusIdx) else 0,
                        reason = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else 0,
                        bytesDownloaded = if (dlIdx >= 0) cursor.getLong(dlIdx) else 0L,
                        bytesTotal = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L,
                        mimeType = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null,
                        lastModified = if (modIdx >= 0) cursor.getLong(modIdx) else 0L
                    )
                )
            }
        }
    } catch (_: Exception) {
        return emptyList()
    }

    return items.sortedByDescending { it.lastModified }
}

fun removeTrackedDownload(context: Context, downloadId: Long) {
    val dm = context.getSystemService(DownloadManager::class.java)
    runCatching { dm?.remove(downloadId) }
    val prefs = context.getSharedPreferences(PREFS_DOWNLOADS, Context.MODE_PRIVATE)
    val existing = prefs.getStringSet(KEY_DOWNLOAD_IDS, emptySet()).orEmpty().toMutableSet()
    existing.remove(downloadId.toString())
    prefs.edit().putStringSet(KEY_DOWNLOAD_IDS, existing).apply()
}

fun openDownloadedFile(context: Context, item: DownloadedItem): Boolean {
    val dm = context.getSystemService(DownloadManager::class.java) ?: return false
    val uri = runCatching { dm.getUriForDownloadedFile(item.id) }.getOrNull() ?: return false
    val mime = item.mimeType ?: dm.getMimeTypeForDownloadedFile(item.id) ?: "*/*"
    return try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        openSystemDownloadsApp(context)
    }
}

fun openSystemDownloadsApp(context: Context): Boolean {
    return try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

fun pinGameOrAppToHomeScreen(context: Context, app: StoreApp): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        return false
    }
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(EXTRA_LAUNCH_APP_URL, app.downloadUrl)
        putExtra(EXTRA_LAUNCH_APP_TITLE, app.title)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    val shortcut = ShortcutInfoCompat.Builder(context, "playhub_${app.id}")
        .setShortLabel(app.title.take(24))
        .setLongLabel(app.title)
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(launchIntent)
        .build()

    return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}

fun pinPlayHubToHomeScreen(context: Context): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        return false
    }
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        addCategory(Intent.CATEGORY_LAUNCHER)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    val shortcut = ShortcutInfoCompat.Builder(context, "playhub_main_shortcut")
        .setShortLabel("Play Hub")
        .setLongLabel("Play Hub Store & Games")
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(launchIntent)
        .build()

    return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}

private fun saveTrackedDownloadId(context: Context, id: Long) {
    val prefs = context.getSharedPreferences(PREFS_DOWNLOADS, Context.MODE_PRIVATE)
    val existing = prefs.getStringSet(KEY_DOWNLOAD_IDS, emptySet()).orEmpty().toMutableSet()
    existing.add(id.toString())
    prefs.edit().putStringSet(KEY_DOWNLOAD_IDS, existing).apply()
}

private fun getTrackedDownloadIds(context: Context): List<Long> {
    val prefs = context.getSharedPreferences(PREFS_DOWNLOADS, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_DOWNLOAD_IDS, emptySet())
        .orEmpty()
        .mapNotNull { it.toLongOrNull() }
}
