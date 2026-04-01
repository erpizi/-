package com.example.spotifyfloatlyrics.data

import com.example.spotifyfloatlyrics.model.LyricsResult
import com.example.spotifyfloatlyrics.model.SearchLyricsItem
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object LyricsRepository {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun fetch(title: String, artist: String): LyricsResult? {
        val query = URLEncoder.encode("$title $artist", Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("https://lrclib.net/api/search?q=$query")
            .header("User-Agent", "SpotifyFloatLyrics/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            val items = json.decodeFromString<List<SearchLyricsItem>>(body)
            val best = items.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                ?: items.firstOrNull { !it.plainLyrics.isNullOrBlank() }
                ?: return null

            val plain = best.plainLyrics ?: best.syncedLyrics ?: return null
            return LyricsResult(plainLyrics = plain, syncedLyrics = best.syncedLyrics)
        }
    }
}
