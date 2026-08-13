package com.notify.android.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.notify.android.tv.theme.NotifyPurple
import com.notify.core.model.Track
import com.notify.core.ui.imageUrl
import com.notify.core.ui.player.PlayerViewModel

/** Spotify TV-style track row list. Focus + D-pad friendly. */
@Composable
fun TvTrackList(
    tracks: List<Track>,
    playerVm: PlayerViewModel
) {
    Column(Modifier.fillMaxWidth()) {
        tracks.forEachIndexed { index, track ->
            TvTrackRow(
                track = track,
                index = index + 1,
                isCurrent = playerVm.isCurrent(track),
                isPlaying = playerVm.playing.collectAsState().value,
                onPlay = { playerVm.playQueue(tracks, index) }
            )
        }
    }
}

@Composable
fun TvTrackRow(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (focused) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onPlay)
            .onFocusChanged { focused = it.hasFocus }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null,
                    tint = NotifyPurple,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(index.toString(), color = Color(0xFFB3B3B3), fontSize = 14.sp)
            }
        }
        val art = imageUrl(track.displayArt)
        if (art != null) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A2A))) {
                AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (isCurrent) Color.White else Color(0xFFE5E5E5),
                fontSize = 16.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.displayArtist.isNotEmpty()) {
                Text(track.displayArtist, color = Color(0xFFB3B3B3), fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun ArtworkPlaceholder(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF2A2A2A)).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFF7A7A7A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
