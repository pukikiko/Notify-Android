package com.notify.android.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notify.android.mobile.ui.theme.NotifyPurple

@Composable
fun MediaCard(
    imageUrl: String?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    rounded: Boolean = false
) {
    val shape: Shape = if (rounded) CircleShape else RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Artwork(
                url = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape),
                shape = shape
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChipRow(
    items: List<String>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit = {}
) {
    Row(modifier = modifier) {
        items.take(3).forEach { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2A2A))
                    .clickable { onClick(text) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun HeartIconSmall(liked: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = if (liked) "Liked" else "Like",
        tint = if (liked) NotifyPurple else Color(0xFFB3B3B3),
        modifier = modifier.size(20.dp)
    )
}
