package com.notify.android.mobile.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.mobile.ui.components.Artwork
import com.notify.android.mobile.ui.components.formatDurationMs
import com.notify.android.mobile.ui.components.hashHue
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.viewmodels.imageUrl
import com.notify.core.player.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerVm: PlayerViewModel,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val current by playerVm.current.collectAsState()
    val playing by playerVm.playing.collectAsState()
    val preparing by playerVm.preparing.collectAsState()
    val buffering by playerVm.buffering.collectAsState()
    val position by playerVm.position.collectAsState()
    val duration by playerVm.duration.collectAsState()
    val shuffle by playerVm.shuffle.collectAsState()
    val repeat by playerVm.repeat.collectAsState()
    val queue by playerVm.queue.collectAsState()
    val queueIndex by playerVm.index.collectAsState()

    var showQueue by remember { mutableStateOf(false) }

    val track = current
    val artUrl = playerVm.currentArtUrl()

    val hue = hashHue((track?.title ?: "") + (track?.displayArtist ?: ""))
    val topColor = androidx.compose.ui.graphics.Color.hsv(
        hue = hue,
        saturation = 0.45f,
        value = 0.28f
    )
    val midColor = androidx.compose.ui.graphics.Color.hsv(
        hue = hue,
        saturation = 0.30f,
        value = 0.16f
    )

    Box(Modifier.fillMaxSize()) {
        // Spotify's artwork-tinted dynamic background
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(topColor, midColor, Color(0xFF121212))))
        )
        Column(Modifier.fillMaxSize()) {
            // Header: back + title (queue button lives at the bottom right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Now Playing", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB3B3B3))
                    Text(
                        if (track != null) listOfNotNull(track.displayAlbum.ifBlank { null }).joinToString(" · ") else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB3B3B3)
                    )
                }
                // Keeps the header title centered now that the queue button
                // lives at the bottom right of the screen.
                Box(Modifier.size(48.dp))
            }

            // Square artwork centered in the remaining space
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val artSize = minOf(maxWidth, maxHeight)
                Artwork(
                    url = artUrl,
                    contentDescription = track?.title,
                    modifier = Modifier.size(artSize),
                    shape = RoundedCornerShape(12.dp)
                )
                if (preparing || buffering) {
                    Box(
                        Modifier
                            .size(artSize)
                            .background(Color(0x66000000), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            // Title + artist + like
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val artistId = playerVm.currentArtistId()
                    Text(
                        track?.displayArtist ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFB3B3B3),
                        modifier = Modifier
                            .clickable(enabled = artistId != null) { artistId?.let(onOpenArtist) }
                    )
                }
                if (track != null && track.id.toLongOrNull() != null) {
                    IconButton(onClick = { playerVm.toggleLikeCurrent() }) {
                        Icon(
                            if (track.liked == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            if (track.liked == true) "Liked" else "Like",
                            tint = if (track.liked == true) Color(0xFF8F5CFF) else Color(0xFFB3B3B3),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Seek bar
            if (track != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatDurationMs(position), style = MaterialTheme.typography.labelSmall, color = Color(0xFFB3B3B3))
                    Slider(
                        value = position.toFloat(),
                        onValueChange = { playerVm.seekTo(it.toLong()) },
                        valueRange = 0f..if (duration > 0) duration.toFloat() else 1f,
                        enabled = duration > 0,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF4D4D4D)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text(
                        if (duration > 0) formatDurationMs(duration) else "--:--",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB3B3B3)
                    )
                }
            }

            // Transport controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    icon = Icons.Filled.Shuffle,
                    label = "Shuffle",
                    active = shuffle,
                    onClick = { playerVm.toggleShuffle() },
                    modifier = Modifier.size(40.dp)
                )
                ControlButton(
                    icon = Icons.Filled.SkipPrevious,
                    label = "Previous",
                    onClick = { playerVm.previous() },
                    modifier = Modifier.size(48.dp)
                )
                FilledIconButton(
                    onClick = { playerVm.toggle() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF8F5CFF),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }
                ControlButton(
                    icon = Icons.Filled.SkipNext,
                    label = "Next",
                    onClick = { playerVm.next() },
                    modifier = Modifier.size(48.dp)
                )
                ControlButton(
                    icon = if (repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    label = "Repeat",
                    active = repeat != RepeatMode.OFF,
                    onClick = { playerVm.cycleRepeat() },
                    modifier = Modifier.size(40.dp)
                )
            }

            // Queue button, bottom right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "Up next", tint = Color.White)
                }
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = Color(0xFF282828),
            contentColor = Color.White
        ) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Up next",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                if (queue.isEmpty()) {
                    Text(
                        "Nothing in the queue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB3B3B3),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                } else {
                    queue.forEachIndexed { index, t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerVm.loadIndex(index)
                                    showQueue = false
                                }
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Artwork(
                                url = imageUrl(t.displayArt),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (index == queueIndex) Color(0xFF8F5CFF) else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    t.displayArtist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB3B3B3),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = if (active) Color(0xFF8F5CFF) else Color.White),
        modifier = modifier
    ) {
        Icon(icon, label)
    }
}
