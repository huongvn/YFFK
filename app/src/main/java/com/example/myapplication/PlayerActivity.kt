package com.example.myapplication

import android.os.Bundle
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

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@PlayerActivity.youTubePlayer = youTubePlayer
                playCurrent(youTubePlayer)
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                if (state == PlayerConstants.PlayerState.ENDED && videoIds.isNotEmpty()) {
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
    }

    private fun handleRemoteAction(action: String) {
        val player = youTubePlayer ?: return
        when (action) {
            "play" -> player.play()
            "stop" -> player.pause()
            "next" -> {
                if (videoIds.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % videoIds.size
                    playCurrent(player)
                }
            }
        }
    }

    private fun playCurrent(youTubePlayer: YouTubePlayer) {
        val id = videoIds.getOrNull(currentIndex) ?: return
        youTubePlayer.loadVideo(id, 0f)
    }
}
