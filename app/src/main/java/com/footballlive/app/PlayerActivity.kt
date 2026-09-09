package com.footballlive.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player)

        val playerView =
            findViewById<PlayerView>(R.id.playerView)

        val streamUrl =
            intent.getStringExtra("url")

        if (streamUrl.isNullOrBlank()) {
            finish()
            return
        }

        player = ExoPlayer.Builder(this).build()

        playerView.player = player

        val mediaItem =
            MediaItem.fromUri(streamUrl)

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onStop() {
        super.onStop()

        player?.release()
        player = null
    }
}
