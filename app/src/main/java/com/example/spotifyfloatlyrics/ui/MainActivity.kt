package com.example.spotifyfloatlyrics.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.spotifyfloatlyrics.databinding.ActivityMainBinding
import com.example.spotifyfloatlyrics.service.OverlayLyricsService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOverlayPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        binding.btnNotificationPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnStart.setOnClickListener {
            val intent = Intent(this, OverlayLyricsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
            updateStatus()
        }

        binding.btnStop.setOnClickListener {
            stopService(Intent(this, OverlayLyricsService::class.java))
            updateStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        val listenerOk = isNotificationServiceEnabled(this)
        binding.tvStatus.text = "状态：悬浮窗=" + yesNo(overlayOk) + "，通知读取=" + yesNo(listenerOk)
    }

    private fun yesNo(value: Boolean): String = if (value) "已开启" else "未开启"

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(context, com.example.spotifyfloatlyrics.service.SpotifyNotificationListener::class.java)
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }
}
