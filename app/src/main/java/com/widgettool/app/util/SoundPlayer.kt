package com.widgettool.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build

class SoundPlayer private constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    companion object {
        @Volatile
        private var instance: SoundPlayer? = null

        fun getInstance(context: Context): SoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: SoundPlayer(context.applicationContext).also { instance = it }
            }
        }
    }

    fun playSound(uri: Uri, onComplete: (() -> Unit)? = null) {
        release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    release()
                    onComplete?.invoke()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete?.invoke()
        }
    }

    fun playSound(resId: Int, onComplete: (() -> Unit)? = null) {
        release()
        try {
            mediaPlayer = MediaPlayer.create(context, resId).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setOnCompletionListener {
                    release()
                    onComplete?.invoke()
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete?.invoke()
        }
    }

    fun release() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            reset()
            release()
        }
        mediaPlayer = null
    }

}
