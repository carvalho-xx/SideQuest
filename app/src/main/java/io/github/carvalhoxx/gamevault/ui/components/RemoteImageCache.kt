package io.github.carvalhoxx.gamevault.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object RemoteImageCache {

    private val memoryCache = object : LruCache<String, ImageBitmap>(60) {}

    fun getFromMemory(url: String): ImageBitmap? = synchronized(memoryCache) {
        memoryCache.get(url)
    }

    suspend fun loadOrFetch(context: Context, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return@withContext null

        getFromMemory(cleanUrl)?.let { return@withContext it }

        val cacheDir = File(context.cacheDir, "game_covers").apply { mkdirs() }
        val cacheFile = File(cacheDir, hashUrl(cleanUrl))

        if (cacheFile.exists() && cacheFile.length() > 0L) {
            val diskBitmap = runCatching {
                BitmapFactory.decodeFile(cacheFile.absolutePath)?.asImageBitmap()
            }.getOrNull()
            if (diskBitmap != null) {
                synchronized(memoryCache) { memoryCache.put(cleanUrl, diskBitmap) }
                return@withContext diskBitmap
            }
        }

        val bytes = fetchBytes(cleanUrl) ?: return@withContext null
        val decoded = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull() ?: return@withContext null

        runCatching {
            cacheFile.writeBytes(bytes)
        }
        synchronized(memoryCache) {
            memoryCache.put(cleanUrl, decoded)
        }
        decoded
    }

    private fun fetchBytes(urlStr: String): ByteArray? {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7_000
                readTimeout = 7_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "GameVault/1.0 Android")
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { it.readBytes() }
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun hashUrl(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".img"
    }
}

@Composable
fun rememberRemoteImageBitmap(url: String?): ImageBitmap? {
    val cleanUrl = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val context = LocalContext.current
    var bitmap by remember(cleanUrl) {
        mutableStateOf(RemoteImageCache.getFromMemory(cleanUrl))
    }

    LaunchedEffect(cleanUrl) {
        if (bitmap == null) {
            bitmap = RemoteImageCache.loadOrFetch(context, cleanUrl)
        }
    }
    return bitmap
}
