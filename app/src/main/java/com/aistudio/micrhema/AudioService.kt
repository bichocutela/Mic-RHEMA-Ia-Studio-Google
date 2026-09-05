package com.aistudio.micrhema

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList

class AudioService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var screenReceiverRegistered = false
    private val xpHandler = Handler(Looper.getMainLooper())
    private val xpProgressRunnable = object : Runnable {
        override fun run() {
            val player = mediaSession?.player
            if (player != null && player.isPlaying) {
                val mediaUrl = player.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
                if (mediaUrl.isNotBlank()) {
                    XpMediaClient.recordAudio(
                        context = this@AudioService,
                        audioUrl = mediaUrl,
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                        isActive = true
                    )
                }
            }
            xpHandler.postDelayed(this, 4_000L)
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (
                intent.action == Intent.ACTION_SCREEN_OFF &&
                !UserSettingsManager.getStoredSettings(this@AudioService).continuePlaybackWhenLocked
            ) {
                mediaSession?.player?.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val settings = UserSettingsManager.getStoredSettings(this)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(settings.skipTime.coerceIn(10, 30) * 1000L)
            .setSeekForwardIncrementMs(settings.skipTime.coerceIn(10, 30) * 1000L)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    ExoPlayerCache.getCacheDataSourceFactory(this)
                )
            )
            .build()

        val rewindButton = CommandButton.Builder()
            .setPlayerCommand(Player.COMMAND_SEEK_BACK)
            .setIconResId(R.drawable.ic_rewind_10)
            .setDisplayName("Voltar ${settings.skipTime.coerceIn(10, 30)}s")
            .build()

        val forwardButton = CommandButton.Builder()
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .setIconResId(R.drawable.ic_forward_30)
            .setDisplayName("Avançar ${settings.skipTime.coerceIn(10, 30)}s")
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCustomLayout(ImmutableList.of(rewindButton, forwardButton))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
                    val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon().build()

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .setCustomLayout(ImmutableList.of(rewindButton, forwardButton))
                        .build()
                }
            })
            .build()

        xpHandler.post(xpProgressRunnable)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }
        screenReceiverRegistered = true
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        xpHandler.removeCallbacks(xpProgressRunnable)
        if (screenReceiverRegistered) {
            unregisterReceiver(screenOffReceiver)
            screenReceiverRegistered = false
        }
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
