package com.notify.android.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.notify.android.mobile.ui.theme.SpotifySurfaceHigh

/** Loads artwork. Image URLs may be absolute (Spotify/CDN) or relative to the
 *  active instance — resolved by the caller before passing in. */
@Composable
fun Artwork(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(modifier = modifier.clip(shape).background(SpotifySurfaceHigh)) {
        if (url.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize().background(SpotifySurfaceHigh)) {
                NoteIcon(modifier = Modifier.size(28.dp).align(androidx.compose.ui.Alignment.Center))
            }
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

@Composable
private fun NoteIcon(modifier: Modifier = Modifier) {
    androidx.compose.material3.Icon(
        imageVector = Icons.Rounded.MusicNote,
        contentDescription = null,
        tint = Color(0xFF7A7A7A),
        modifier = modifier
    )
}
