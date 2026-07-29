package com.aistudio.micrhema

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ContentAudioPlayerViewModel : ViewModel() {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()
    
    private val _currentAudio = MutableStateFlow<ContentAudio?>(null)
    val currentAudio = _currentAudio.asStateFlow()

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    fun initialize(context: Context) {
        if (mediaController != null) return

        try {
            val serviceIntent = Intent(context, AudioService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sessionToken = SessionToken(context, ComponentName(context, AudioService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        mediaControllerFuture?.addListener({
            try {
                val controller = mediaControllerFuture?.get()
                mediaController = controller
                
                controller?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        _isBuffering.value = (state == Player.STATE_BUFFERING)
                        if (state == Player.STATE_READY) {
                            _duration.value = controller.duration.coerceAtLeast(0L)
                        }
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                    }
                })
                
                // Start polling progress
                viewModelScope.launch {
                    while (isActive) {
                        if (controller?.isPlaying == true) {
                            _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
                            _duration.value = controller.duration.coerceAtLeast(0L)
                        }
                        delay(500)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    fun playAudio(audio: ContentAudio) {
        val controller = mediaController ?: return
        
        // Save previous progress if any
        _currentAudio.value?.let { prev ->
            prev.progress = if (_duration.value > 0) (_currentPosition.value.toFloat() / _duration.value.toFloat()) else 0f
            prev.lastPosition = _currentPosition.value
        }
        
        _currentAudio.value = audio
        
        val uri = if (audio.isCached) {
            // Find local path if downloaded
            audio.audioUrl // Assuming cache resolves this or it's a local file. ExoPlayerCache handles cached URLs
        } else {
            audio.audioUrl
        }
        
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(audio.title)
                    .setArtist(audio.artist)
                    .setArtworkUri(android.net.Uri.parse(audio.coverUrl))
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        if (audio.lastPosition > 0) {
            controller.seekTo(audio.lastPosition)
        }
        controller.play()
        _playbackSpeed.value = 1.0f
        controller.playbackParameters = PlaybackParameters(1.0f)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun seekForward30() {
        val controller = mediaController ?: return
        val newPos = (controller.currentPosition + 30000).coerceAtMost(controller.duration)
        controller.seekTo(newPos)
        _currentPosition.value = newPos
    }

    fun seekBackward10() {
        val controller = mediaController ?: return
        val newPos = (controller.currentPosition - 10000).coerceAtLeast(0)
        controller.seekTo(newPos)
        _currentPosition.value = newPos
    }

    fun cycleSpeed() {
        val controller = mediaController ?: return
        val nextSpeed = when (_playbackSpeed.value) {
            0.75f -> 1.0f
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            else -> 0.75f
        }
        _playbackSpeed.value = nextSpeed
        controller.playbackParameters = PlaybackParameters(nextSpeed)
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val nextMode = if (controller.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        controller.repeatMode = nextMode
    }

    fun stop() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _currentAudio.value = null
    }

    override fun onCleared() {
        super.onCleared()
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
