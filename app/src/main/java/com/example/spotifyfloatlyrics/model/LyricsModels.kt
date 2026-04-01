package com.example.spotifyfloatlyrics.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchLyricsItem(
    @SerialName("trackName") val trackName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("albumName") val albumName: String? = null,
    @SerialName("plainLyrics") val plainLyrics: String? = null,
    @SerialName("syncedLyrics") val syncedLyrics: String? = null
)

data class LyricsResult(
    val plainLyrics: String,
    val syncedLyrics: String? = null
)

data class TimedLine(
    val timeMs: Long,
    val text: String
)
