package com.notify.android.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.tv.theme.NotifyPurple

@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.width(420.dp)) {
        Text(label, color = Color(0xFFB3B3B3), fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) Color.White else Color(0xFF4D4D4D),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(Color(0xFF181818), RoundedCornerShape(8.dp))
                .onFocusChanged { focused = it.hasFocus }
                .padding(14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TvInputButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .background(if (primary) NotifyPurple else Color(0xFF2A2A2A), RoundedCornerShape(50))
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

@Composable
fun TvHeartButton(liked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(56.dp)
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.25f)
                    liked -> Color.White.copy(alpha = 0.12f)
                    else -> Color(0xFF2A2A2A)
                },
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.hasFocus },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            if (liked) "Following" else "Follow",
            tint = if (liked) NotifyPurple else Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun TvPrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .border(
                width = if (focused) 4.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .background(if (enabled) NotifyPurple else Color(0xFF4D4D4D), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .onFocusChanged { focused = it.hasFocus }
            .padding(horizontal = 40.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
