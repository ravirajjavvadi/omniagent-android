package com.omniagent.app.ui.features.splash

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.omniagent.app.R

/**
 * Animated Splash Screen featuring the OmniAgent intro video via ExoPlayer (Media3).
 * Professional video engine for maximum clarity and hardware-accelerated rendering.
 */
@OptIn(UnstableApi::class)
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    
    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.omniagent}")
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            
            // Listen for completion
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onSplashFinished()
                    }
                }
            })
        }
    }

    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Hide play/pause controls
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill screen with clarity
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
