package com.zacharee1.boredsigns.services

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.MediaVolumeWidget
import com.zacharee1.boredsigns.widgets.RingerVolumeWidget

class VolumeService : Service() {
    companion object {
        val BASE = "com.zacharee1.boredsigns.action.VOLUME"

        val ACTION_RINGER_UP = BASE + "_RINGER_UP"
        val ACTION_RINGER_DOWN = BASE + "_RINGER_DOWN"

        val ACTION_MEDIA_UP = BASE + "_MEDIA_UP"
        val ACTION_MEDIA_DOWN = BASE + "_MEDIA_DOWN"

        val FILTER = object : IntentFilter() {
            init {
                addAction(ACTION_RINGER_UP)
                addAction(ACTION_RINGER_DOWN)
                addAction(ACTION_MEDIA_UP)
                addAction(ACTION_MEDIA_DOWN)
                addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_RINGER_UP) {
                turnUpRinger()
            }

            if (intent?.action == ACTION_RINGER_DOWN) {
                turnDownRinger()
            }

            if (intent?.action == ACTION_MEDIA_UP) {
                turnUpMedia()
            }

            if (intent?.action == ACTION_MEDIA_DOWN) {
                turnDownMedia()
            }

            if (intent?.action == NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED) {
                updateAll()
            }
        }
    }

    private val systemObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri!!.toString().contains("volume") || uri == Settings.Global.getUriFor(Settings.Global.ZEN_MODE)) {
                updateAll()
            }
        }
    }

    private lateinit var audioManager: AudioManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, systemObserver)
        contentResolver.registerContentObserver(Settings.Global.CONTENT_URI, true, systemObserver)

        Settings.System.VOLUME_VOICE

        registerReceiver(receiver, FILTER)
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {}

        contentResolver.unregisterContentObserver(systemObserver)
    }

    private fun turnUpRinger() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, currentVolume + 1, 0)
        }

        Utils.sendWidgetUpdate(this, RingerVolumeWidget::class.java, null)
    }

    private fun turnDownRinger() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        if (audioManager.ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, currentVolume - 1, 0)
        } else {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
        Utils.sendWidgetUpdate(this, RingerVolumeWidget::class.java, null)
    }

    private fun turnUpMedia() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume + 5, 0)
    }

    private fun turnDownMedia() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume - 5, 0)
    }

    private fun updateAll() {
        Utils.sendWidgetUpdate(this, RingerVolumeWidget::class.java, null)
        Utils.sendWidgetUpdate(this, MediaVolumeWidget::class.java, null)
    }
}
