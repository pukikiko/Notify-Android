package com.notify.android.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.notify.core.model.Artist
import com.notify.core.ui.appContainer
import com.notify.core.ui.navigation.navId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_MS = 6000L

/** Full-bleed featured-artist hero (webapp ArtistShowcase). The artist artwork
    fills the whole hero and the slide content sits directly on it — no card. */
@Composable
fun TvArtistShowcase(
    artists: List<Artist>,
    onOpenArtist: (String) -> Unit
) {
    if (artists.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    var anyFocus by remember { mutableStateOf(false) }
    var heroFocused by remember { mutableStateOf(false) }
    val count = artists.size

    LaunchedEffect(anyFocus, count) {
        if (anyFocus || count <= 1) return@LaunchedEffect
        while (true) {
            delay(AUTO_MS)
            index = (index + 1) % count
        }
    }

    val artist = artists[index.coerceIn(0, count - 1)]
    val container = appContainer()
    val api = container.api
    val session = container.session
    val bio = artist.bio

    val markFocus = { f: Boolean -> anyFocus = f }

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    // Album artwork per artist, fetched once on mount (reliably served by the
    // instance; the wiki images may be blocked by the network).
    var albumBgs by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    LaunchedEffect(Unit) {
        artists.forEach { a ->
            launch {
                val res = runCatching { api.artistDetail(a.id) }
                val img = res.getOrNull()?.albums?.firstNotNullOfOrNull { it.image }?.let { session.imageUrl(it) }
                if (img != null) albumBgs = albumBgs + (a.id to img)
            }
        }
    }

    val albumBg = albumBgs[artist.id]
    val wikiBg = artist.wikiImage?.let { session.imageUrl(it) }

    // Background artwork: wiki image first (hero); if it fails to load we fall
    // back to the album cover, then the portrait, then the hue gradient.
    var wikiFailed by remember(artist.id) { mutableStateOf(false) }
    val bg = listOfNotNull(
        if (wikiFailed) null else wikiBg,
        albumBg,
        artist.image?.let { session.imageUrl(it) }
    ).firstOrNull()
    val hue = hashHue(artist.name)

    // Portrait artwork: artist's own photo, falling back to the album cover.
    val portrait = artist.image?.let { session.imageUrl(it) }
        ?: albumBg
        ?: wikiBg

    val backgroundModifier = if (bg != null) {
        Modifier.background(Color(0xFF181818))
    } else {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color.hsv(hue, 0.55f, 0.30f),
                    Color.hsv(hue, 0.45f, 0.18f),
                    Color(0xFF181818)
                )
            )
        )
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(420.dp)
            .then(backgroundModifier)
            .border(
                width = if (heroFocused) 4.dp else 0.dp,
                color = if (heroFocused) Color.White else Color.Transparent
            )
            .clickable { onOpenArtist(artist.navId()) }
            .onFocusChanged {
                heroFocused = it.hasFocus
                markFocus(it.hasFocus)
            }
    ) {
        // Artist artwork fills the entire hero background
        if (bg != null) {
            AsyncImage(
                model = bg,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { wikiFailed = true }
            )
            // Scrim for legibility
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0x73000000), Color(0x26000000), Color(0x59000000))))
            )
        }

        // Prev arrow
        ShowcaseArrow(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            label = "Previous artist",
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
        ) {
            markFocus(true)
            index = (index - 1 + count) % count
        }

        Column(Modifier.fillMaxSize()) {
            // Greeting sits at the top of the hero, on the artwork
            Text(
                greeting,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 88.dp, top = 26.dp)
            )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 88.dp, end = 88.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (portrait != null) {
                AsyncImage(
                    model = portrait,
                    contentDescription = artist.name,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        artist.name.take(1),
                        color = Color(0xFF7A7A7A),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 32.dp)
            ) {
                Text(
                    "Popular artist",
                    color = Color(0xE6FFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    artist.name,
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
                val meta = buildList {
                    if (artist.genres.isNotEmpty()) add(artist.genres.take(3).joinToString(" · "))
                    if ((artist.trackCount ?: 0) > 0) add("${artist.trackCount} songs")
                    if ((artist.albumCount ?: 0) > 0) add("${artist.albumCount} albums")
                }.joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        color = Color(0xE6FFFFFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (!bio.isNullOrBlank()) {
                    Text(
                        bio,
                        color = Color(0xEBFFFFFF),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            }
        }

        }

        // Next arrow
        ShowcaseArrow(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            label = "Next artist",
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
        ) {
            markFocus(true)
            index = (index + 1) % count
        }

        // Dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            artists.forEachIndexed { i, a ->
                val active = i == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(if (active) 22.dp else 8.dp)
                        .height(8.dp)
                        .background(if (active) Color.White else Color(0x66FFFFFF), RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun ShowcaseArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(40.dp)
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .background(Color(0x8C000000), CircleShape)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}
