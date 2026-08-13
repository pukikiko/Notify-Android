package com.notify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SoulseekStatus(
    val connected: Boolean = false,
    val mode: String? = null,
    val username: String? = null
)

@Serializable
data class SourceStatus(
    val enabled: Boolean = false,
    val mode: String? = null,
    val binary: String? = null
)

@Serializable
data class CacheStatus(
    @SerialName("availableTracks") val availableTracks: Long = 0,
    val downloading: Long = 0,
    @SerialName("originalBytes") val originalBytes: Long = 0,
    @SerialName("transcodedBytes") val transcodedBytes: Long = 0
)

@Serializable
data class Status(
    val soulseek: SoulseekStatus = SoulseekStatus(),
    val sources: Map<String, SourceStatus> = emptyMap(),
    val cache: CacheStatus = CacheStatus(),
    val formats: Map<String, String> = emptyMap()
)

@Serializable
data class UserCounts(
    @SerialName("likedTracks") val likedTracks: Long = 0,
    @SerialName("likedArtists") val likedArtists: Long = 0,
    @SerialName("likedAlbums") val likedAlbums: Long = 0,
    val playlists: Long = 0
)

@Serializable
data class DownloadsResponse(
    val downloads: List<Track> = emptyList()
)
