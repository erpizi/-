package com.example.spotifyfloatlyrics.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.spotifyfloatlyrics.model.NowPlayingSong
import com.example.spotifyfloatlyrics.util.SongBus

class SpotifyNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName != SPOTIFY_PACKAGE) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE)?.trim().orEmpty()
        val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val album = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

        if (title.isBlank() || artist.isBlank()) return
        SongBus.update(NowPlayingSong(title = clean(title), artist = clean(artist), album = album))
    }

    private fun clean(value: String): String {
        return value
            .replace(Regex("\\s*[-–—]\\s*Remaster(ed)?\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Live.*?\\)", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    companion object {
        private const val SPOTIFY_PACKAGE = "com.spotify.music"
    }
}
