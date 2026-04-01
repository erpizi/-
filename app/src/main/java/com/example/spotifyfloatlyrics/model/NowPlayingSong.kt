package com.example.spotifyfloatlyrics.model

data class NowPlayingSong(
    val title: String,
    val artist: String,
    val album: String? = null
)
