package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val youTubePlayerView = YouTubePlayerView(this)
        setContentView(youTubePlayerView)

        lifecycle.addObserver(youTubePlayerView)

        val videoId = intent.getStringExtra("VIDEO_ID") ?: ""

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadVideo(videoId, 0f)
            }

            override fun onError(
                youTubePlayer: YouTubePlayer?,
                errorReason: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.errors YouTubePlayer.ErrorReason?
            ) {
                super.onError(youTubePlayer, errorReason)
                android.util.Log.e("YT_PLAYER_ERROR", "Error reason: ${errorReason?.code} - ${errorReason?.reason}")
            }
        })
    }
}