package com.notify.android.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.tv.theme.NotifyPurple
import com.notify.android.tv.ui.components.ArtworkPlaceholder
import com.notify.android.tv.ui.components.formatDurationMs
import com.notify.android.tv.ui.components.hashHue
import com.notify.core.player.RepeatMode
import com.notify.core.ui.imageUrl
import com.notify.core.ui.player.PlayerViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun TvNowPlayingScreen(playerVm: PlayerViewModel) {
    val current by playerVm.current.collectAsState()
    val playing by playerVm.playing.collectAsState()
    val position by playerVm.position.collectAsState()
    val duration by playerVm.duration.collectAsState()
    val shuffle by playerVm.shuffle.collectAsState()
    val repeat by playerVm.repeat.collectAsState()

    // Move D-pad focus into the overlay the moment it appears so the remote
    // can control playback without tabbing through the screen underneath.
    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Retry: the button may not be attached to the focus owner on the first frame.
        repeat(3) {
            playFocusRequester.requestFocus()
            delay(50)
        }
    }

    val track = current ?: return
    val artUrl = playerVm.currentArtUrl()

    val hue = hashHue(track.title + track.displayArtist)
    val tint = Color.hsv(hue, 0.45f, 0.28f)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Full-bleed blurred artwork background
        if (!artUrl.isNullOrBlank()) {
            AsyncImage(
                model = artUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(30.dp)
                    .background(Color(0xFF121212)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(tint, Color(0xFF121212))))
            )
        }
        Box(Modifier.fillMaxSize().background(Color(0x99000000)))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 56.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Now Playing", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Notify", color = NotifyPurple, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }

            Row(verticalAlignment = Alignment.Bottom) {
                if (!artUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artUrl,
                        contentDescription = track.title,
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    ArtworkPlaceholder(track.title, Modifier.size(180.dp))
                }
                Column(
                    modifier = Modifier.padding(start = 36.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(track.title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(track.displayArtist, color = Color(0xFFB3B3B3), fontSize = 20.sp, maxLines = 1)
                    Text(
                        "Playing on this device",
                        color = Color(0xFFB3B3B3),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column {
                // seek bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDurationMs(position), color = Color(0xFFB3B3B3), fontSize = 13.sp)
                    Slider(
                        value = position.toFloat(),
                        onValueChange = { playerVm.seekTo(it.toLong()) },
                        valueRange = 0f..if (duration > 0) duration.toFloat() else 1f,
                        enabled = duration > 0,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF4D4D4D)
                        )
                    )
                    Text(if (duration > 0) formatDurationMs(duration) else "--:--", color = Color(0xFFB3B3B3), fontSize = 13.sp)
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        ControlButton(Icons.Filled.Shuffle, shuffle, Modifier.size(48.dp)) { playerVm.toggleShuffle() }
                        ControlButton(Icons.Filled.SkipPrevious, false, Modifier.size(48.dp)) { playerVm.previous() }
                        PlayPauseButton(
                            playing = playing,
                            focusRequester = playFocusRequester,
                            modifier = Modifier.size(64.dp),
                            onClick = { playerVm.toggle() }
                        )
                        ControlButton(Icons.Filled.SkipNext, false, Modifier.size(48.dp)) { playerVm.next() }
                        ControlButton(if (repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, repeat != RepeatMode.OFF, Modifier.size(48.dp)) { playerVm.cycleRepeat() }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Press BACK to close",
                    color = Color(0xFFB3B3B3),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .scale(if (focused) 1.15f else 1f)
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .background(if (focused) Color.White.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) NotifyPurple else Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun PlayPauseButton(
    playing: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .scale(if (focused) 1.15f else 1f)
            .border(
                width = if (focused) 4.dp else 3.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50)
            )
            .background(NotifyPurple, RoundedCornerShape(50))
            .focusRequester(focusRequester)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (playing) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(36.dp)
        )
    }
}
