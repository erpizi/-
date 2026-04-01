package com.example.spotifyfloatlyrics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.spotifyfloatlyrics.R
import com.example.spotifyfloatlyrics.data.LyricsRepository
import com.example.spotifyfloatlyrics.model.TimedLine
import com.example.spotifyfloatlyrics.ui.MainActivity
import com.example.spotifyfloatlyrics.util.LrcParser
import com.example.spotifyfloatlyrics.util.SongBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayLyricsService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var lyricsView: TextView
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createNotificationChannel()
        startForeground(1001, buildNotification("悬浮歌词运行中"))
        setupOverlay()
        observeSongs()
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lyricsView = TextView(this).apply {
            text = "等待 Spotify 播放中..."
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            setShadowLayer(12f, 0f, 0f, 0xCC000000.toInt())
            setPadding(24, 20, 24, 20)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setBackgroundColor(0x33000000)
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 220
        }

        lyricsView.setOnTouchListener(DragTouchListener())
        windowManager.addView(lyricsView, params)
    }

    private fun observeSongs() {
        serviceScope.launch {
            SongBus.song.filterNotNull().collect { song ->
                lyricsView.text = "正在搜索：${song.title} - ${song.artist}"
                syncJob?.cancel()
                syncJob = launch {
                    val result = withContext(Dispatchers.IO) {
                        LyricsRepository.fetch(song.title, song.artist)
                    }
                    if (result == null) {
                        lyricsView.text = "未找到歌词\n${song.title} - ${song.artist}"
                        return@launch
                    }

                    val synced = result.syncedLyrics
                    if (!synced.isNullOrBlank()) {
                        runSyncedLyrics(LrcParser.parse(synced))
                    } else {
                        lyricsView.text = result.plainLyrics.take(240)
                    }
                }
            }
        }
    }

    private suspend fun runSyncedLyrics(lines: List<TimedLine>) {
        if (lines.isEmpty()) {
            lyricsView.text = "同步歌词为空"
            return
        }
        val startedAt = System.currentTimeMillis()
        var index = 0
        while (index < lines.size) {
            val elapsed = System.currentTimeMillis() - startedAt
            val current = lines[index]
            if (elapsed >= current.timeMs) {
                val next = lines.getOrNull(index + 1)?.text.orEmpty()
                lyricsView.text = buildString {
                    append(current.text)
                    if (next.isNotBlank()) {
                        append("\n")
                        append(next)
                    }
                }
                index++
            } else {
                delay(50)
            }
        }
    }

    private fun buildNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Spotify Float Lyrics")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spotify Float Lyrics",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        serviceScope.cancel()
        if (::lyricsView.isInitialized) {
            runCatching { windowManager.removeView(lyricsView) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class DragTouchListener : android.view.View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f

        override fun onTouch(v: android.view.View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(lyricsView, params)
                    return true
                }
            }
            return false
        }
    }

    companion object {
        private const val CHANNEL_ID = "spotify_float_lyrics"
    }
}
