package com.notify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Playlist(
    @Serializable(with = FlexibleIdSerializer::class) val id: String = "",
    val name: String,
    val description: String? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("trackCount") val trackCount: Int? = null,
    val duration: Long? = null,
    @SerialName("coverTrackId") val coverTrackId: String? = null,
    // Spotify/discover playlist fields
    val owner: String? = null,
    @SerialName("ownerId") val ownerId: String? = null,
    val followers: Int? = null,
    @SerialName("public") val isPublic: Boolean? = null,
    @SerialName("spotifyUrl") val spotifyUrl: String? = null,
    val image: String? = null,
    val mbid: String? = null,
    val kind: String? = null,
    val source: JsonElement? = null
)

@Serializable
data class PlaylistsResponse(
    val playlists: List<Playlist>
)

@Serializable
data class PlaylistResponse(
    val playlist: Playlist,
    val tracks: List<Track> = emptyList()
)

@Serializable
data class PlaylistActionResponse(
    @SerialName("ok") val ok: Boolean = true,
    val playlist: Playlist? = null
)
