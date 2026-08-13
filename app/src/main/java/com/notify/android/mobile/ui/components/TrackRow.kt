package com.notify.android.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notify.core.ui.player.PlayerViewModel
import com.notify.android.mobile.ui.theme.NotifyPurple
import com.notify.android.mobile.ui.theme.SpotifySurfaceHigh
import com.notify.core.model.Track

/** One Spotify-style track row. Rows that are currently playing show a
 *  pause/play glyph; a small download badge indicates an in-flight download. */
@Composable
fun TrackRow(
    track: Track,
    index: Int? = null,
    artUrl: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onOpenArtist: (() -> Unit)? = null,
    onOpenAlbum: (() -> Unit)? = null,
    showArtist: Boolean = true,
    showAlbum: Boolean = false,
    showIndex: Boolean = true,
    like: (() -> Unit)? = null,
    liked: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    subtitleLine: (@Composable () -> Unit)? = null,
    highlight: Boolean = false
) {
    val bg = if (highlight) Color(0xFF2A2A2A) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // index / play indicator
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (index != null && showIndex && !isCurrent) {
                Text(
                    index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB3B3B3)
                )
            }
            if (isCurrent) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = NotifyPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (artUrl != null) {
            Artwork(
                url = artUrl,
                contentDescription = track.title,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) Color.White else Color(0xFFE5E5E5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitleLine != null) {
                subtitleLine()
            } else {
                Row {
                    if (track.status == "downloading") {
                        Icon(Icons.Default.Download, null, tint = NotifyPurple, modifier = Modifier.size(12.dp))
                        Text(
                            " downloading",
                            style = MaterialTheme.typography.bodySmall,
                            color = NotifyPurple,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    val subtitle = buildString {
                        if (showArtist && track.displayArtist.isNotEmpty()) append(track.displayArtist)
                        if (showAlbum && track.displayAlbum.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append(track.displayAlbum)
                        }
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB3B3B3),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (like != null) {
            IconButton(onClick = like, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (liked) NotifyPurple else Color(0xFFB3B3B3),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        trailing?.invoke()
    }
}
