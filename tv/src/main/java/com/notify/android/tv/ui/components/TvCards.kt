package com.notify.android.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.notify.android.tv.theme.NotifyPurple
import com.notify.android.tv.theme.SpotifySurfaceHigh

/** Focus-aware card for TV D-pad navigation. */
@Composable
fun TvCard(
    imageUrl: String?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    rounded: Boolean = false,
    width: Dp = 220.dp,
    onFocus: ((Boolean) -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.07f else 1f,
        animationSpec = tween(120),
        label = "tvcard-scale"
    )
    val artShape = RoundedCornerShape(if (rounded) 50 else 8, 8, 8, 8)

    Column(
        modifier = modifier
            .width(width)
            .scale(scale)
            .then(
                if (focused) Modifier.background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { st ->
                focused = st.hasFocus
                onFocus?.invoke(st.hasFocus)
            }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(artShape)
                .background(SpotifySurfaceHigh)
        ) {
            if (imageUrl.isNullOrBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, null, tint = Color(0xFF7A7A7A), modifier = Modifier.size(40.dp))
                }
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(
            title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (subtitle != null) {
            Text(
                subtitle,
                color = Color(0xFFB3B3B3),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvSectionTitle(title: String) {
    Text(
        title,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 40.dp, top = 28.dp, bottom = 12.dp)
    )
}

@Composable
fun TvTabRow(tabs: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.padding(start = 40.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { label ->
            TvTab(label, label == selected) { onSelect(label) }
        }
    }
}

@Composable
private fun TvTab(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .padding(end = 24.dp)
            .background(
                if (selected) Color(0xFF3A3A3A)
                else if (focused) Color.White.copy(alpha = 0.25f)
                else Color.Transparent,
                RoundedCornerShape(50)
            )
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (selected || focused) Color.White else Color(0xFFB3B3B3),
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TvPlayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(56.dp)
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .background(NotifyPurple, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.PlayArrow, "Play", tint = Color.Black, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun TvTextButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .background(
                color = if (primary) NotifyPurple else Color(0xFF2A2A2A),
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus }
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            color = if (primary) Color.Black else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
