package org.zotero.android.speech

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource

@UnstableApi
class RemoteAudioPlayer(private val context: Context) {
    interface Listener {
        fun onPlaybackFinished()
        fun onPlaybackError(error: PlaybackException)
    }

    var listener: Listener? = null

    private var player: ExoPlayer? = null
    private var hasMedia = false

    var speed: Float = 1f
        set(value) {
            field = value
            player?.playbackParameters = PlaybackParameters(value, 1f)
        }

    val canResume: Boolean
        get() = hasMedia

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    val progress: Float
        get() {
            val player = player ?: return 0f
            val duration = player.duration
            if (!hasMedia || duration <= 0) {
                return 0f
            }
            return (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        }

    val elapsedTime: Double
        get() = if (hasMedia) (player?.currentPosition ?: 0L) / 1000.0 else 0.0

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                listener?.onPlaybackFinished()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            listener?.onPlaybackError(error)
        }
    }

    fun play(data: ByteArray) {
        val exoPlayer = player ?: createPlayer()
        val mediaSource = ProgressiveMediaSource
            .Factory(DataSource.Factory { ByteArrayDataSource(data) })
            .createMediaSource(MediaItem.fromUri(Uri.parse(segmentUri)))
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.playbackParameters = PlaybackParameters(speed, 1f)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        hasMedia = true
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
        hasMedia = false
    }

    fun release() {
        player?.removeListener(playerListener)
        player?.release()
        player = null
        hasMedia = false
    }

    private fun createPlayer(): ExoPlayer {
        val exoPlayer = ExoPlayer.Builder(context).build()
        exoPlayer.addListener(playerListener)
        player = exoPlayer
        return exoPlayer
    }

    private companion object {
        const val segmentUri = "zotero-read-aloud://segment"
    }
}