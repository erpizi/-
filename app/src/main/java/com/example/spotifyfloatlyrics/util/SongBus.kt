package com.example.spotifyfloatlyrics.util

import com.example.spotifyfloatlyrics.model.NowPlayingSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SongBus {
    private val _song = MutableStateFlow<NowPlayingSong?>(null)
    val song: StateFlow<NowPlayingSong?> = _song.asStateFlow()

    fun update(song: NowPlayingSong) {
        _song.value = song
    }
}
