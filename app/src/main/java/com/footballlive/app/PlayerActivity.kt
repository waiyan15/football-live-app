package com.footballlive.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_player
        )

        val playerView =
            findViewById<PlayerView>(
                R.id.playerView
            )

        val progress =
            findViewById<ProgressBar>(
                R.id.playerProgress
            )

        val errorText =
            findViewById<TextView>(
                R.id.playerError
            )

        val channelTitle =
            findViewById<TextView>(
                R.id.channelTitle
            )

        val liveBadge =
            findViewById<TextView>(
                R.id.playerLiveBadge
            )

        val backButton =
            findViewById<ImageButton>(
                R.id.playerBackButton
            )

        val streamUrl =
            intent.getStringExtra("url")

        val channelName =
            intent.getStringExtra("name")
                ?: "Football Live"

        channelTitle.text =
            channelName

        liveBadge.text =
            "● LIVE"

        backButton.setOnClickListener {
            finish()
        }

        if (streamUrl.isNullOrBlank()) {

            errorText.text =
                "Stream URL မရှိပါ"

            errorText.visibility =
                View.VISIBLE

            liveBadge.visibility =
                View.GONE

            return
        }

        try {

            val dataSourceFactory =
                DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(
                        true
                    )
                    .setConnectTimeoutMs(
                        15000
                    )
                    .setReadTimeoutMs(
                        20000
                    )

            player =
                ExoPlayer
                    .Builder(this)
                    .build()

            playerView.player =
                player

            val uri =
                Uri.parse(streamUrl)

            val isHls =
                streamUrl.contains(
                    ".m3u8",
                    true
                ) ||
                streamUrl.contains(
                    "m3u8",
                    true
                )

            if (isHls) {

                val mediaSource =
                    HlsMediaSource.Factory(
                        dataSourceFactory
                    ).createMediaSource(
                        MediaItem.fromUri(uri)
                    )

                player?.setMediaSource(
                    mediaSource
                )

            } else {

                player?.setMediaItem(
                    MediaItem.fromUri(uri)
                )
            }

            player?.addListener(
                object : Player.Listener {

                    override fun onPlaybackStateChanged(
                        state: Int
                    ) {

                        when (state) {

                            Player.STATE_BUFFERING -> {

                                progress.visibility =
                                    View.VISIBLE
                            }

                            Player.STATE_READY -> {

                                progress.visibility =
                                    View.GONE

                                errorText.visibility =
                                    View.GONE

                                liveBadge.visibility =
                                    View.VISIBLE
                            }

                            Player.STATE_ENDED -> {

                                progress.visibility =
                                    View.GONE
                            }
                        }
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        progress.visibility =
                            View.GONE

                        errorText.visibility =
                            View.VISIBLE

                        errorText.text =
                            "Video ဖွင့်မရပါ\n\n${error.errorCodeName}"

                        liveBadge.visibility =
                            View.GONE
                    }
                }
            )

            player?.prepare()

            player?.playWhenReady =
                true

        } catch (e: Exception) {

            progress.visibility =
                View.GONE

            errorText.visibility =
                View.VISIBLE

            errorText.text =
                "Player Error\n\n${e.message}"

            liveBadge.visibility =
                View.GONE
        }
    }

    override fun onStop() {
        super.onStop()

        player?.release()

        player = null
    }
}
