package com.notify.android.mobile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.notify.core.data.DownloadState
import com.notify.core.data.OfflineCollection
import com.notify.core.model.Track
import com.notify.core.ui.offlineViewModel

/** Collection-level offline download control (Liked Songs / playlist / album).
 *  Shows Download → Downloading… → Downloaded based on the offline index and
 *  any in-flight downloads for these tracks. */
@Composable
fun DownloadCollectionButton(
    tracks: List<Track>,
    collection: OfflineCollection,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return

    val vm = offlineViewModel()
    val downloaded by vm.tracks.collectAsState()
    val downloads by vm.downloads.collectAsState()

    val downloadedCount = downloaded.count { it.collection?.key == collection.key }
    val fullyDownloaded = downloadedCount >= tracks.size
    val downloading = downloads.values.any {
        it.state == DownloadState.DOWNLOADING && tracks.any { t -> t.id == it.trackId }
    }

    OutlinedButton(
        onClick = { vm.downloadCollection(tracks, collection) },
        enabled = !downloading && !fullyDownloaded,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = modifier
    ) {
        Icon(
            if (fullyDownloaded) Icons.Filled.Check else Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Text(
            when {
                downloading -> "Downloading…"
                fullyDownloaded -> "Downloaded"
                else -> "Download"
            },
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
