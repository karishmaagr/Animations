package com.karishma.swiggyanimation.music

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal object ItunesRepository {

    suspend fun fetchTracks(query: String = "top hits pop"): List<Track> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://itunes.apple.com/search?term=$encoded&entity=song&limit=25")
        runCatching {
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val results = JSONObject(body).getJSONArray("results")
            (0 until results.length()).mapNotNull { i ->
                val obj = results.getJSONObject(i)
                val previewUrl = obj.optString("previewUrl").takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                Track(
                    id = obj.optLong("trackId"),
                    title = obj.optString("trackName", "Unknown"),
                    artist = obj.optString("artistName", "Unknown Artist"),
                    album = obj.optString("collectionName", "Unknown Album"),
                    artworkUrl = obj.optString("artworkUrl100", "").replace("100x100bb", "600x600bb"),
                    previewUrl = previewUrl,
                    duration = obj.optLong("trackTimeMillis", 30_000L),
                )
            }
        }.getOrElse { emptyList() }
    }

    suspend fun loadArtwork(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            BitmapFactory.decodeStream(conn.inputStream).also { conn.disconnect() }
        }.getOrNull()
    }
}
