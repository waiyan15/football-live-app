package com.footballlive.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player)

        val playerView = findViewById<PlayerView>(R.id.playerView)

        val streamUrl = intent.getStringExtra("url")

        if (streamUrl.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Stream URL မရှိပါ",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        try {
            player = ExoPlayer.Builder(this).build()

            playerView.player = player

            val mediaItem = MediaItem.fromUri(streamUrl)

            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Player ဖွင့်မရပါ",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
