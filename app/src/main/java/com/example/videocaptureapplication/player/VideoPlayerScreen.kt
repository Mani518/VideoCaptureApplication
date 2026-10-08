package com.example.videocaptureapplication.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    videoUri: Uri,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0L)
                if (state == Player.STATE_ENDED) isPlaying = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // keep the seek bar moving
    LaunchedEffect(player) {
        while (true) {
            if (!isSeeking) position = player.currentPosition
            delay(300)
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(text = title, color = Color.White)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false      // we draw our own controls below
                    }
                }
            )
        }

        // ---- custom controls ----
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Slider(
                value = if (isSeeking) seekValue else {
                    if (duration > 0) position.toFloat() / duration else 0f
                },
                onValueChange = {
                    isSeeking = true
                    seekValue = it
                },
                onValueChangeFinished = {
                    player.seekTo((seekValue * duration).toLong())
                    isSeeking = false
                }
            )

            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(position), color = Color.White)
                Box(Modifier.weight(1f))
                Text(formatTime(duration), color = Color.White)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Back 10 seconds", tint = Color.White)
                }

                IconButton(onClick = {
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                    if (player.isPlaying) player.pause() else player.play()
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or pause",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                IconButton(onClick = { player.seekTo((player.currentPosition + 10_000).coerceAtMost(duration)) }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}
