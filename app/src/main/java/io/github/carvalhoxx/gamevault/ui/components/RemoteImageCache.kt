package io.github.carvalhoxx.gamevault.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
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
import kotlin.math.abs

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
                BitmapFactory.decodeFile(cacheFile.absolutePath)
                    ?.let(::trimLetterboxBorders)
                    ?.asImageBitmap()
            }.getOrNull()
            if (diskBitmap != null) {
                synchronized(memoryCache) { memoryCache.put(cleanUrl, diskBitmap) }
                return@withContext diskBitmap
            }
        }

        val bytes = fetchBytes(cleanUrl) ?: return@withContext null
        val decoded = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.let(::trimLetterboxBorders)
                ?.asImageBitmap()
        }.getOrNull() ?: return@withContext null

        runCatching {
            cacheFile.writeBytes(bytes)
        }
        synchronized(memoryCache) {
            memoryCache.put(cleanUrl, decoded)
        }
        decoded
    }

    /**
     * Remove faixas sólidas de letterbox/pillarbox embutidas na própria imagem
     * (ex.: capas do HLTB/RAWG que vêm em canvas 2:3 com barras sólidas em cima e embaixo),
     * permitindo que o ContentScale.Crop preencha 100% da área reservada no Compose.
     */
    internal fun trimLetterboxBorders(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        if (w < 32 || h < 32) return source

        fun isUniformBorderRow(y: Int, refPixel: Int): Boolean {
            val refR = AndroidColor.red(refPixel)
            val refG = AndroidColor.green(refPixel)
            val refB = AndroidColor.blue(refPixel)
            val step = (w / 16).coerceAtLeast(1)
            var x = 0
            while (x < w) {
                val px = source.getPixel(x, y)
                if (abs(AndroidColor.red(px) - refR) > 14 ||
                    abs(AndroidColor.green(px) - refG) > 14 ||
                    abs(AndroidColor.blue(px) - refB) > 14
                ) {
                    return false
                }
                x += step
            }
            return true
        }

        val topRef = source.getPixel(w / 2, 0)
        var topInset = 0
        val maxTrim = (h * 0.35f).toInt()
        while (topInset < maxTrim && isUniformBorderRow(topInset, topRef)) {
            topInset++
        }

        val bottomRef = source.getPixel(w / 2, h - 1)
        var bottomInset = 0
        while (bottomInset < maxTrim && isUniformBorderRow(h - 1 - bottomInset, bottomRef)) {
            bottomInset++
        }

        // Só recorta quando houver faixas de letterbox claras (> 12px) em cima e embaixo
        if (topInset < 12 && bottomInset < 12) return source

        val newHeight = h - topInset - bottomInset
        if (newHeight < h * 0.3f) return source

        return runCatching {
            Bitmap.createBitmap(source, 0, topInset, w, newHeight)
        }.getOrDefault(source)
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
