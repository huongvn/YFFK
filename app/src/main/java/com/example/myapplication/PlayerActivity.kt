package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.mqtt.MqttController
import com.example.myapplication.mqtt.PlaybackCommandBus
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class PlayerActivity : AppCompatActivity() {

    private var youTubePlayer: YouTubePlayer? = null
    private var videoIds: List<String> = emptyList()
    private var currentIndex = 0
    private var isPlaying = false
    private var limitNotified = false

    private val limitHandler = Handler(Looper.getMainLooper())
    private val limitRunnable = object : Runnable {
        override fun run() {
            if (SessionTimer.isOverLimit()) {
                if (isPlaying) {
                    youTubePlayer?.pause()
                    isPlaying = false
                }
                if (!limitNotified) {
                    Toast.makeText(this@PlayerActivity, "Bạn đã xem quá số phút cho phép", Toast.LENGTH_LONG).show()
                    limitNotified = true
                }
            } else {
                limitNotified = false
            }
            limitHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val youTubePlayerView = YouTubePlayerView(this)
        setContentView(youTubePlayerView)
        lifecycle.addObserver(youTubePlayerView)

        videoIds = intent.getStringArrayListExtra("VIDEO_IDS")?.filter { it.isNotEmpty() }
            ?: run {
                val single = intent.getStringExtra("VIDEO_ID") ?: ""
                listOf(single)
            }
        currentIndex = intent.getIntExtra("INDEX", 0)

        PlaybackCommandBus.listener = { action -> handleRemoteAction(action) }
        limitHandler.post(limitRunnable)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@PlayerActivity.youTubePlayer = youTubePlayer
                playCurrent(youTubePlayer)
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                when (state) {
                    PlayerConstants.PlayerState.PLAYING -> isPlaying = true
                    PlayerConstants.PlayerState.PAUSED,
                    PlayerConstants.PlayerState.ENDED,
                    PlayerConstants.PlayerState.UNSTARTED,
                    PlayerConstants.PlayerState.VIDEO_CUED -> isPlaying = false
                    else -> { }
                }
                if (state == PlayerConstants.PlayerState.ENDED && videoIds.isNotEmpty() && !SessionTimer.isOverLimit()) {
                    currentIndex = (currentIndex + 1) % videoIds.size
                    playCurrent(youTubePlayer)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        PlaybackCommandBus.listener = null
        MqttController.onState = null
        limitHandler.removeCallbacks(limitRunnable)
    }

    private fun handleRemoteAction(action: String) {
        val player = youTubePlayer ?: return
        when (action) {
            "play" -> {
                if (SessionTimer.isOverLimit()) {
                    notifyLimit()
                } else {
                    player.play()
                }
            }
            "stop" -> player.pause()
            "next" -> {
                if (videoIds.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % videoIds.size
                    playCurrent(player)
                }
            }
        }
    }

    private fun notifyLimit() {
        if (!limitNotified) {
            Toast.makeText(this, "Bạn đã xem quá số phút cho phép", Toast.LENGTH_LONG).show()
            limitNotified = true
        }
    }

    private fun playCurrent(youTubePlayer: YouTubePlayer) {
        if (SessionTimer.isOverLimit()) {
            notifyLimit()
            return
        }
        val id = videoIds.getOrNull(currentIndex) ?: return
        youTubePlayer.loadVideo(id, 0f)
    }
}
